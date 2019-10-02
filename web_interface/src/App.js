import React, { Component } from 'react'
import SD from 'simplerdux'
import CreateSession from './views/CreateSession'
import Manager from './views/manager/Manager'
import SelectDatabase from './views/SelectDatabase'

export class App extends Component {

  render() {
    const {database, connectionId} = SD.getState()

    return database ? 
      connectionId ? <Manager /> : <CreateSession />
      :
      <SelectDatabase />
  }
}

export default App
