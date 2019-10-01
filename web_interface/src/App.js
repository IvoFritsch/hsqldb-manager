import React, { Component } from 'react'
import SD from 'simplerdux'
import CreateSessionMessage from './views/CreateSessionMessage'
import Manager from './views/manager/Manager'
import SelectDatabase from './views/SelectDatabase'

export class App extends Component {
  state = {
    webSession: true
  }

  render() {
    const {database} = SD.getState()
    const {webSession} = this.state

    return webSession ? 
      database ? <Manager /> : <SelectDatabase /> 
      :
      <CreateSessionMessage />
  }
}

export default App
