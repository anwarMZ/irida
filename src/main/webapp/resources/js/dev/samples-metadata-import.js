/* eslint new-cap: ["error", { "properties": false }] */
var $ = require('jquery');
import Dropzone from 'dropzone';

/**
 * Create a table from the metadata return after parsing the excel docs.
 * @return {object} datatable creation method
 */
const tableCreator = (function() {
  /**
   * Create an object of the headers title and text.
   * @param {array} headers list of table headers
   * @return {array} of formatted data for table headers.
   */
  function transformHeaders(headers) {
    return headers.map(function(h) {
      return {title: h, data: h};
    });
  }

  /**
   * Crate the datatable
   * @param {array} data for the table
   */
  return function createTable(data) {
    var nameCol = data.headers.indexOf('NLEP #');
    var headers = transformHeaders(data.headers);
    $('#metadata-table').DataTable({
      scrollX: true,
      data: data.metadata,
      columns: headers,
      createdRow: function(row, data) {
        if (data.sampleId === '') {
          $(row).addClass('bg-danger');
        }
      },
      columnDefs: [
        {
          targets: nameCol,
          render: function(data, type, row) {
            var id = row.sampleId;
            if (id === '') {
              return data;
            }
            return `<a href="/samples/${id}">${data}</a>`;
          }
        }
      ]
    });
  };
})();

Dropzone.options.metadataDropzone = {
  paramName: 'file',
  acceptedFiles: '.xlx,.xlsx',
  init: function() {
    var data;
    this.on('success', function(file, result) {
      data = result;
    });
    this.on('complete', function() {
      $('#metadataDropzone').fadeOut(200, function() {
        $(this).remove();
        tableCreator(data);
      });
    });
  }
};
