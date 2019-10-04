import React, { Component } from 'react'
import {List, ListItem, ListItemText, Paper, IconButton, Icon} from '@material-ui/core'
import SD from 'simplerdux'
import HWApiFetch from 'hw-api-fetch'

export class TablesList extends Component {
  state = {
    tables: undefined
  }

  mustRender = true;

  componentDidMount() {
    this.getTables()
    SD.setState({refreshTables: this.getTables})
  }
  
  getTables = async () => {
    const {connectionId} = SD.getState()
    const {status, tables} = await HWApiFetch.get(`metadata/${connectionId}`)
    if(status !== 'OK') return
    this.mustRender = true;
    this.setState({tables})
  }

  selectToQuery = (table) => {
    const {focusEditor, setSql} = SD.getState()
    if(setSql) setSql(`select * from ${table};`)
    if(focusEditor) focusEditor()
  }

  customContextMenu = (e, table) => {
    e.preventDefault()
    SD.setState({contextMenu: {
      table,
      x: e.pageX,
      y: e.pageY,
    }})
  }

  shouldComponentUpdate(nexProps) {
    if(this.props.uncommittedWork !== nexProps.uncommittedWork) this.mustRender = true
    return this.mustRender;
  }

  render() {
    const {database} = SD.getState()
    const {tables} = this.state
    const {uncommittedWork} = this.props
    this.mustRender = false;
    
    return (
      <>
        <Paper className={`database-name-container ${uncommittedWork && 'uncommitted'}`}>
          <span>
            {database}
          </span>
          <IconButton onClick={this.getTables}>
            <Icon>refresh</Icon>
          </IconButton>
        </Paper>
        <div style={{overflowY:'auto', overflowX:'hidden', maxHeight:'100%'}}>
          <List className='list-tables-container' component='nav'>
            {tables && tables.map(t => 
              <ListItem 
                key={t.name} 
                onClick={() => this.selectToQuery(t.name.toLowerCase())} 
                onContextMenu={e => this.customContextMenu(e, t.name.toLowerCase())}
                button 
              >
                <ListItemText primary={t.name.toLowerCase()} />
              </ListItem>
            )}
          </List>
        </div>
      </>
    )
  }
}

export default TablesList
