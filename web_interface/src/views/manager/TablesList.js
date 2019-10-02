import React, { Component } from 'react'
import {List, ListItem, ListItemText, Paper, IconButton, Icon} from '@material-ui/core'
import SD from 'simplerdux'
import HWApiFetch from 'hw-api-fetch'

export class TablesList extends Component {
  state = {
    tables: undefined
  }

  componentDidMount() {
    this.getTables()
    SD.setState({refreshTables: this.getTables})
  }
  
  getTables = async () => {
    const {connectionId} = SD.getState()
    const {status, tables} = await HWApiFetch.get(`metadata/${connectionId}`)
    if(status !== 'OK') return
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

  render() {
    const {database} = SD.getState()
    const {tables} = this.state

    return (
      <>
        <Paper className='database-name-container'>
          <span>
            {database}
          </span>
          <IconButton onClick={this.getTables}>
            <Icon>refresh</Icon>
          </IconButton>
        </Paper>
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
      </>
    )
  }
}

export default TablesList
