import React, { Component } from 'react'
import {ClickAwayListener, Paper, List, ListItem, ListItemText} from '@material-ui/core'
import SD from 'simplerdux'

export class CustomContextMenu extends Component {
  options = ['Select', 'Insert', 'Update', 'Delete']

  setQuery = (option, table) => {
    const {focusEditor} = SD.getState()
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

    SD.setState({sql: query}, true)
    this.close()
    if(focusEditor) focusEditor()
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
