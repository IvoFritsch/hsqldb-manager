import React, { Component } from 'react'
import SD from 'simplerdux'
import {Grid} from '@material-ui/core'
import HWApiFetch from 'hw-api-fetch'
import hsqldb_logo from '../assets/imgs/hsqldb-logo.png'

export class CreateSession extends Component {

  componentDidMount() {
    this.initInterval = setInterval(this.initSession, 500)
  }

  componentWillUnmount() {
    clearInterval(this.initInterval)
  }

  initSession = async () => {
    const {database} = SD.getState()
    if(!database) return
    const response = await HWApiFetch.get(`init/${database}`)
    if(response.status !== 'OK') return
    SD.setState({connectionId: response.connectionId})
    clearInterval(this.initInterval)
  }

  render() {
    return (
      <Grid container justify='center' alignItems='center' direction='column' className='new-web-session-container'>
        <img src={hsqldb_logo} alt='hsqldb logo' />
        <h2>Run `hsqlman webtool permit` to continue.</h2>
      </Grid>
    )
  }
}

export default CreateSession
