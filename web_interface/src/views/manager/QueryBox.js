import React, { Component } from 'react'
import {Fab, Icon} from '@material-ui/core'
import Editor from 'react-simple-code-editor'
import Prism from 'prismjs'
import SD from 'simplerdux'
import 'prismjs/components/prism-sql'
import 'prismjs/themes/prism.css'

export class QueryBox extends Component {

  componentDidMount() {
    SD.setState({focusEditor: this.focusEditor, runQuery: this.runQuery})
  }

  focusEditor = () => {
    const editorContainer = document.getElementById('editor-container')
    if(!editorContainer) return
    const textarea = editorContainer.getElementsByTagName('textarea')[0]
    if(textarea) textarea.focus()
  }

  runQuery = () => {
    SD.setState({isLoadingQuery: true})
    console.log('Run: \n\n' + SD.getState().sql)
    this.focusEditor()
    setTimeout(() => SD.setState({isLoadingQuery: false}), 1000)
  }

  render() {
    const {sql=''} = SD.getState()
    
    return (
      <>
        <Editor
          id='editor-container'
          value={sql}
          onValueChange={sql => SD.setState({sql}, true)}
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
