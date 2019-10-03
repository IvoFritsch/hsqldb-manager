import React, { Component } from 'react'
import SplitPane from 'react-split-pane'
import SD from 'simplerdux'
import QueryBox from './QueryBox'
import QueryResults from './QueryResults'
import TablesList from './TablesList'
import CustomContextMenu from './CustomContextMenu'
import HWApiFetch from 'hw-api-fetch'

export class Manager extends Component {

  componentDidMount() {
    this.keepAlive()
    setInterval(this.keepAlive, 40000)
    document.addEventListener('keydown', this.shortcutBehaviors)
  }

  componentWillUnmount() {
    document.removeEventListener('keydown', this.shortcutBehaviors)
  }

  keepAlive = async () => {
    const {connectionId} = SD.getState()
    await HWApiFetch.get(`keepalive/${connectionId}`)
  }

  shortcutBehaviors = e => {
    if(!e.ctrlKey) return
    if(e.keyCode === 69 || e.keyCode === 13) {
      const {runQuery} = SD.getState()
      e.returnValue = false
      if(runQuery) runQuery()
      return false
    }
    
    if(e.keyCode === 38) {
      const {historyQuery} = SD.getState()
      if(historyQuery) historyQuery(1)
    }
    
    if(e.keyCode === 40) {
      const {historyQuery} = SD.getState()
      if(historyQuery) historyQuery(0)
    }
  }

  render() {
    const {h_pane_position=200, v_pane_position=280, contextMenu, isLoadingQuery, executionTime} = SD.getState()

    return (
      <>
        <CustomContextMenu {...contextMenu} />
        <SplitPane 
          split='vertical'
          defaultSize={v_pane_position}
          onChange={size => SD.setState({v_pane_position: size}, true)}
          minSize={120}
          maxSize={-120}
          pane1Style={{marginBottom: '29px', paddingTop:'60px'}}
        >
          <TablesList />
          <SplitPane 
            split='horizontal'
            defaultSize={h_pane_position}
            onChange={size => SD.setState({h_pane_position: size}, true)}
            minSize={120}
            maxSize={-120}
            pane2Style={{marginBottom: '29px'}}
          >
            <QueryBox />
            <QueryResults />
          </SplitPane>
        </SplitPane>
        
      
        <div style={{position:'fixed', width: '100%', backgroundColor:'#4caf50', padding:'5px 20px', bottom: '0px', color: 'white', zIndex: '10', height: '19px'}}>
          {isLoadingQuery ? "Executing..." : (executionTime ? `Query executed in ${(executionTime / 100000).toFixed(2)} ms | Ready to execute` : 'Ready to execute')}
        </div>
      </>
    )
  }
}

export default Manager
