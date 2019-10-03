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
    sql: SD.getState().sql || ''
  }

  componentDidMount() {
    SD.setState({setSql: this.setSql, focusEditor: this.focusEditor, runQuery: this.runQuery})
  }

  setSql = (sql='') => this.setState({sql})

  focusEditor = () => {
    const editorContainer = document.getElementById('editor-container')
    if(!editorContainer) return
    const textarea = editorContainer.getElementsByTagName('textarea')[0]
    if(textarea) textarea.focus()
  }

  runQuery = async () => {
    let {sql=''} = this.state
    const {isLoadingQuery, connectionId} = SD.getState()
    
    if(isLoadingQuery) return

    SD.setState({isLoadingQuery: true, rs: undefined, rsUpdateMessage: undefined, rsErrorMessage: undefined})
    this.focusEditor()

    try {
      SD.setState({sql}, true)
      const response = await HWApiFetch.post(`query/${connectionId}`, {sql})
      
      if(response.status === 'RESULT_SET') SD.setState({rs: response.rs})
      if(response.status === 'UPDATE') SD.setState({rsUpdateMessage: response.message})
      if(response.status === 'SQL_ERROR') SD.setState({rsErrorMessage: response.message})
      
      sql = sql.toLowerCase()
      this.saveCommands(sql)
      this.shouldRefreshTables(sql)

    } finally {
      SD.setState({isLoadingQuery: false})
    }
  }

  shouldRefreshTables = (sql) => {
    if(!sql.includes('create') && !sql.includes('alter') && !sql.includes('drop')) return
    const {refreshTables} = SD.getState()
    if(refreshTables) refreshTables()
  }

  saveCommands = (sql) => {
    const commands = ['select', 'insert', 'update', 'delete', 'create', 'alter', 'drop']

    let finished = false

    while(!finished) {

      finished = true
    }
    
    commands.forEach(command => {
      console.log(
      sql.indexOf(command)
      )
    })
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
