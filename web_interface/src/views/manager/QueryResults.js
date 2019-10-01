import React, { Component } from 'react'
import {Table, TableSortLabel, TableHead, TableBody, TableCell, TableRow} from '@material-ui/core';
import SD from 'simplerdux'

export class QueryResults extends Component {
  state = {
    columns: ['id', 'name', 'date'],
    result: [
      [1, 'Ivo Fritsch', '10/10/2018'],
      [2, 'Daniel Schneider', null],
      [3, 'Pit Fritsch', '08/10/2018'],
      [4, 'Carlos Guilherme', '09/10/2018'],
    ],
    sortedResult: undefined,
    sort: {
      index: undefined,
      column: undefined,
      direction: undefined,
    },
  }

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
    const {sort, result} = this.state
    if(!sort.column) return
    this.setState({sortedResult: [...result].sort(this.sortFunction)})
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

  render() {
    const {isLoadingQuery} = SD.getState()
    const {columns, result, sortedResult, sort} = this.state
    
    return (
      <div className={`query-results-container ${isLoadingQuery ? 'loading' : ''}`}>
        <Table size='small'>
          <TableHead>
            <TableRow>
              {columns.map((h, index) => (
                <TableCell key={h}>
                  <TableSortLabel active={sort.column === h} direction={sort.column === h ? sort.direction : undefined} onClick={() => this.sortTable(h, index)}>
                    {h}
                  </TableSortLabel>
                </TableCell>
              ))}
            </TableRow>
          </TableHead>

          <TableBody>
            {((sort.column ? sortedResult: result) || []).map((row, index) =>
              <TableRow hover key={index}>
                {row.map((cell, index)=> 
                  <TableCell key={index}>{cell}</TableCell>
                )}
              </TableRow>
            )}
          </TableBody>
        </Table>
      </div>
    )
  }
}

export default QueryResults