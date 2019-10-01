import React, { Component } from 'react'
import SplitPane from 'react-split-pane'
import SD from 'simplerdux'
import QueryBox from './QueryBox'
import QueryResults from './QueryResults'
import TablesList from './TablesList'
import CustomContextMenu from './CustomContextMenu'

export class Manager extends Component {

  componentDidMount() {
    document.addEventListener('keydown', this.shortcutBehaviors)
  }

  componentWillUnmount() {
    document.removeEventListener('keydown', this.shortcutBehaviors)
  }

  shortcutBehaviors = e => {
    if(!e.ctrlKey) return
    if(e.keyCode === 69 || e.keyCode === 13) {
      const {runQuery} = SD.getState()
      e.returnValue = false
      if(runQuery) runQuery()
      return false
    }
  }

  render() {
    const {h_pane_position, v_pane_position, contextMenu} = SD.getState()

    return (
      <>
        <CustomContextMenu {...contextMenu} />
        <SplitPane 
          split='vertical'
          defaultSize={v_pane_position}
          onChange={size => SD.setState({v_pane_position: size}, true)}
          minSize={120}
          maxSize={-120}
        >
          <TablesList />
          <SplitPane 
            split='horizontal'
            defaultSize={h_pane_position}
            onChange={size => SD.setState({h_pane_position: size}, true)}
            minSize={120}
            maxSize={-120}
          >
            <QueryBox />
            <QueryResults />
          </SplitPane>
        </SplitPane>
      </>
    )
  }
}

export default Manager
