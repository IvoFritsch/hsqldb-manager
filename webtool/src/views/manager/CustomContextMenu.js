import React, { Component } from 'react'
import {ClickAwayListener, Paper, List, ListItem, ListItemText} from '@material-ui/core'
import SD from 'simplerdux'

export class CustomContextMenu extends Component {
  options = ['select', 'insert', 'update', 'delete']

  setQuery = (option, table) => {
    const {focusEditor, setSql} = SD.getState()
    let query = ''

    switch(option.toLowerCase()) {
      case 'insert':
        query = `insert into ${table}\n(...)\nvalues (...);`
        break;
      case 'update':
        query = `update ${table} set;`
        break;
      case 'delete':
        query = `delete from ${table};`
        break;
      default:
        query = `select * from ${table};`
    }

    if(setSql) setSql(query)
    if(focusEditor) focusEditor()
    this.close()
  }

  close = () => SD.setState({contextMenu: undefined})

  render() {
    const {x, y, table} = this.props

    return table ? (
      <ClickAwayListener onClickAway={this.close}>
        <Paper style={{top: y, left: x}} className='context-menu-container'>
          <List>
            {this.options.map(o => 
              <ListItem 
                  key={o} 
                  onClick={() => this.setQuery(o, table)}
                  button 
                >
                  <ListItemText primary={`${o} ... ${table}`} />
              </ListItem>
            )}
          </List>
        </Paper>
      </ClickAwayListener>
    ) : null
  }
}

export default CustomContextMenu
