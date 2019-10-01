import React, { Component } from 'react'
import {Grid} from '@material-ui/core'
import hsqldb_logo from '../assets/imgs/hsqldb-logo.png'

export class CreateSessionMessage extends Component {
  render() {
    return (
      <Grid container justify='center' alignItems='center' direction='column' className='new-web-session-container'>
        <img src={hsqldb_logo} alt='hsqldb logo' />
        <h2>Run `hsqlman websession start` to start the manager</h2>
      </Grid>
    )
  }
}

export default CreateSessionMessage
