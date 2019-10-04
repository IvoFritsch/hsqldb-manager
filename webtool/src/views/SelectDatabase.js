import React, { Component } from 'react'
import SD from 'simplerdux'
import {List, Grid, ListItem, ListItemText} from '@material-ui/core'
import HWApiFetch from 'hw-api-fetch'
import hsqldb_logo from '../assets/imgs/hsqldb-logo.png'

export class SelectDatabase extends Component {
  state = {
    databases: undefined
  }

  interval = undefined;

  componentDidMount() {
    this.interval = setInterval(this.getDatabases, 1000)
  }

  componentWillUnmount(){
    clearInterval(this.interval);
  }
  
  getDatabases = async () => {
    try {
      const databases = await HWApiFetch.get('list')
      this.setState({databases})
    } catch (error) {
    }
  }

  render() {
    const {databases} = this.state

    return (
      <Grid container direction='column' justify='center' alignItems='center' className='select-databases-container'>
        <img src={hsqldb_logo} alt='hsqldb logo' />
        <h1>HSQLDB Manager Webtool</h1>
        
        {!databases &&
          <h2>Run <b>hsqlman webtool start</b> to see the deployed databases.</h2>
        }
        {databases && <>
        <h2>Select Database to continue:</h2>
        <List>
          {databases.map(d => 
            <ListItem
                key={d} 
                onClick={() => SD.setState({database: d})}
                button 
              >
                <ListItemText primary={d} />
            </ListItem>
          )}
        </List>
        </>}
      </Grid>
    )
  }
}

export default SelectDatabase
