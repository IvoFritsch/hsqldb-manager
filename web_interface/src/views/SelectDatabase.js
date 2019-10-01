import React, { Component } from 'react'
import SD from 'simplerdux'
import {List, Grid, ListItem, ListItemText} from '@material-ui/core'

export class SelectDatabase extends Component {
  state = {
    databases: ['flexypoints_server', 'quickmenu', 'mixpad', 'ivo_server']
  }

  render() {
    const {databases} = this.state

    return (
      <Grid container direction='column' justify='center' alignItems='center'>
        <h2>Select Database</h2>
        <hr />
        <List>
          {databases && databases.map(d => 
            <ListItem
                key={d} 
                onClick={() => SD.setState({database: d})}
                button 
              >
                <ListItemText primary={d} />
            </ListItem>
          )}
        </List>
      </Grid>
    )
  }
}

export default SelectDatabase
