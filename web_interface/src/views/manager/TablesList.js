import React, { Component } from 'react'
import {List, ListItem, ListItemText, Paper, IconButton, Icon} from '@material-ui/core'
import SD from 'simplerdux'

export class TablesList extends Component {
  state = {
    tables: ['users', 'pedidos', 'produtos', 'files']
  }

  selectToQuery = (table) => {
    const {focusEditor} = SD.getState()
    SD.setState({sql: `select * from ${table.toLowerCase()};`}, true)
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
          <IconButton>
            <Icon>refresh</Icon>
          </IconButton>
        </Paper>
        <List className='list-tables-container' component='nav'>
          {tables && tables.map(t => 
            <ListItem 
              key={t} 
              onClick={() => this.selectToQuery(t)} 
              onContextMenu={e => this.customContextMenu(e, t)}
              button 
            >
              <ListItemText primary={t} />
            </ListItem>
          )}
        </List>
      </>
    )
  }
}

export default TablesList
