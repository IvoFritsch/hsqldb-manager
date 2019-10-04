import React, { Component } from 'react'
import {Fab, Icon} from '@material-ui/core'
import Editor from 'react-simple-code-editor'
import Prism from 'prismjs'
import SD from 'simplerdux'
import HWApiFetch from 'hw-api-fetch'
import 'prismjs/components/prism-sql'
import 'prismjs/themes/prism.css'

export class QueryBox extends Component {
  state = {
    sql: SD.getState().sql || '',
    historyInfo: undefined
  }
  historyJobActive = false

  componentDidMount() {
    SD.setState({historyQuery: this.historyQuery, setSql: this.setSql, focusEditor: this.focusEditor, runQuery: this.runQuery})
  }

  historyQuery = (direction) => {
    if(this.historyJobActive) return    
    const textarea = this.getTextArea()
    if(!textarea) return

    let {sql='', historyInfo={query: ''}} = this.state
    let {executedQueries=[]} = SD.getState()
    const selection = textarea.selectionStart

    if(historyInfo.selection !== undefined && historyInfo.selection !== selection) historyInfo = {query: ''}
    let queryIndex = historyInfo.queryIndex !== undefined ? historyInfo.queryIndex : executedQueries.length

    if(direction === 0) queryIndex++
    else queryIndex--

    if(queryIndex < 0 || queryIndex >= executedQueries.length) {
      this.setCaretPositionToSelection(textarea, selection)
      return
    }

    this.historyJobActive = true
    const query = executedQueries[queryIndex]
    this.setState({
      sql: `${sql.slice(0, selection)}${query}${sql.slice(selection+historyInfo.query.length)}`,
      historyInfo: { selection, queryIndex, query}
    }, () => this.setCaretPositionToSelection(textarea, selection))
  }

  setCaretPositionToSelection = (textarea, selection) => {
    setTimeout(() => {
      this.historyJobActive = false
      textarea.selectionStart = selection;
      textarea.selectionEnd = selection;
    }, 50);
  }

  setSql = (sql='') => this.setState({sql})

  focusEditor = () => {
    const textarea = this.getTextArea()
    if(textarea) textarea.focus()
  }

  getTextArea = () => {
    const editorContainer = document.getElementById('editor-container')
    if(!editorContainer) return
    return editorContainer.getElementsByTagName('textarea')[0]
  }

  runQuery = async () => {
    let {sql=''} = this.state
    const {isLoadingQuery, connectionId} = SD.getState()
    
    if(isLoadingQuery) return

    SD.setState({isLoadingQuery: true, rs: undefined, rsUpdateMessage: undefined, rsErrorMessage: undefined, executionTime: undefined})
    this.focusEditor()

    try {
      SD.setState({sql}, true)
      const response = await HWApiFetch.post(`query/${connectionId}`, {sql})
      
      if(response.status === 'RESULT_SET') {
        const {setRS} = SD.getState()
        if(setRS) setRS(response.rs)
        SD.setState({executionTime: response.time, qtdRegs: response.rs ? response.rs.data.length : undefined})
      }
      if(response.status === 'UPDATE') SD.setState({rsUpdateMessage: response.message, executionTime: response.time, qtdRegs: undefined})
      if(response.status === 'SQL_ERROR') SD.setState({rsErrorMessage: response.message, qtdRegs: undefined})
      
      sql = sql.toLowerCase()
      this.saveCommands(sql)
      this.shouldRefreshTables(sql)

    } finally {
      SD.setState({isLoadingQuery: false})
    }
  }

  shouldRefreshTables = (sql) => {
    if(!sql.toLowerCase().includes('create') && !sql.toLowerCase().includes('alter') && !sql.toLowerCase().includes('drop')) return
    const {refreshTables} = SD.getState()
    if(refreshTables) refreshTables()
  }

  saveCommands = (sql) => {
    sql = sql.trim()
    const queries = []
    let auxSql = sql
    let finished = false
    let maxTries = 100;
    while(!finished) {
      let startIndex = auxSql.length
      let endIndex = auxSql.length

      startIndex = this.verifyQueryIndex(auxSql, startIndex)
      auxSql = auxSql.substring(startIndex+1)
      endIndex = this.verifyQueryIndex(auxSql, endIndex)
      auxSql = auxSql.substring(endIndex)
      
      queries.push(sql.substring(startIndex, endIndex+1).trim())
      sql = auxSql
      
      if(maxTries <=0 || sql.length === 0) finished = true
      maxTries--
    }

    let {executedQueries=[]} = SD.getState()
    executedQueries = executedQueries.filter(query => !queries.includes(query))
    executedQueries = [...executedQueries, ...queries]
    if(executedQueries.length > 100) executedQueries = executedQueries.slice(executedQueries.length-100)
    SD.setState({executedQueries}, true)
  }

  verifyQueryIndex = (sql, index) => {
    const commands = ['select', 'insert', 'update', 'delete', 'create', 'alter', 'drop']
    for(const command of commands) {
      const i = sql.indexOf(command)
      if(i > -1 && i < index) index = i
    }
    return index
  }

  render() {
    const {sql=''} = this.state
    
    return (
      <>
        <Editor
          id='editor-container'
          value={sql}
          onValueChange={sql => this.setState({sql})}
          highlight={sql => Prism.highlight(sql, Prism.languages.sql)}
          className='query-box-editor'
        />
        
        <Fab title='Run (ctrl + e / ctrl + enter)' onClick={this.runQuery} className='run-sql-button'>
          <Icon>play_arrow</Icon>
        </Fab>
      </>
    )
  }
}

export default QueryBox
