import React, { Component } from 'react'
import {Table, TableSortLabel, TableHead, TableBody, TableCell, TableRow} from '@material-ui/core';
import SD from 'simplerdux'

export class QueryResults extends Component {
  state = {
    rs: undefined,
    sortedResult: undefined,
    sort: {
      index: undefined,
      column: undefined,
      direction: undefined,
    },
    renderedTable: null
  }

  componentDidMount() {
    SD.setState({setRS: this.setRS})
  }

  componentDidUpdate(prevProps, prevState) {
    if(this.state.rs !== prevState.rs) this.setState({
      sortedResult: undefined,
      sort: {
        index: undefined,
        column: undefined,
        direction: undefined,
      }
    })

    if(this.state.rs !== prevState.rs ||
       this.state.sortedResult !== prevState.sortedResult ||
       this.state.sort !== prevState.sort) this.renderRS()
  }

  setRS = (rs) => this.setState({rs})

  sortTable = (column, index) => {
    const {sort} = this.state
    const newSort = {
      index: undefined,
      column: undefined,
      direction: undefined,
    }
    
    if(sort.column !== column) {
      newSort.column = column
      newSort.index = index
      newSort.direction = 'asc'
    } else if(sort.direction === 'asc') {
      newSort.column = column
      newSort.index = index
      newSort.direction = 'desc'
    }

    this.setState({sort: newSort}, this.sortTableData)
  }

  sortTableData = () => {
    const {rs, sort} = this.state
    if(!sort.column || !rs.data) return
    this.setState({sortedResult: [...rs.data].sort(this.sortFunction)})
  }

  sortFunction = (a, b) => {
    const {index, direction} = this.state.sort
    let aAux = a[index] || ''
    let bAux = b[index] || ''
    
    if(isNaN(parseFloat(aAux)) || isNaN(parseFloat(bAux))) {
      aAux = aAux.toLowerCase()
      bAux = bAux.toLowerCase()

      if(aAux > bAux) return direction === 'asc' ? 1 : -1
      else return direction === 'asc' ? -1 : 1
    }

    return direction === 'asc' ? 
      aAux - bAux : 
      bAux - aAux
  }

  renderRS = () => {
    const {rs, sortedResult, sort} = this.state
    if(!rs) return

    this.setState({renderedTable: (
      <Table size='small'>
        <TableHead>
          <TableRow>
            {(rs.headers || []).map((h, index) => (
              <TableCell key={h}>
                <TableSortLabel className='query-results-header' active={sort.column === h} direction={sort.column === h ? sort.direction : undefined} onClick={() => this.sortTable(h, index)}>
                  {h} <h6>({rs.types[index]})</h6>
                </TableSortLabel>
              </TableCell>
            ))}
          </TableRow>
        </TableHead>

        <TableBody>
          {((sort.column ? sortedResult : rs.data) || []).map((row, index) =>
            <TableRow hover key={index}>
              {row.map((cell, index)=> 
                <TableCell key={index} className='result-cell' title={cell}>{cell}</TableCell>
              )}
            </TableRow>
          )}
        </TableBody>
      </Table>
    )})
  }

  render() {
    const {isLoadingQuery, rsUpdateMessage, rsErrorMessage} = SD.getState()
    const {renderedTable: RenderedTable} = this.state
    
    return (
      <>
        <div className={`query-results-container ${isLoadingQuery ? 'loading' : ''}`}>
          {rsUpdateMessage &&
            <div className='query-sql-update'>
              <h5>sql info</h5>
              <h4>{rsUpdateMessage}</h4>
            </div>
          }
          {rsErrorMessage &&
            <div className='query-sql-error'>
              <h5>sql error</h5>
              <h4>{rsErrorMessage}</h4>
            </div>
          }
          {!rsUpdateMessage && !rsErrorMessage && RenderedTable}
        </div>
      </>
    )
  }
}

export default QueryResults