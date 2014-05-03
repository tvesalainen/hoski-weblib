/*
 * Copyright (C) 2012 Helsingfors Segelklubb ry
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
$(function() {
    
    var barcodeservice = $("meta[name='defaults']").attr("content");
  
    $.getJSON(barcodeservice , function(data) {
        if (data) {
            var canvas = document.getElementById("barcode");
            canvas.width = data.barwidth+10;
            var cntx = canvas.getContext("2d");
            cntx.clearRect(0, 0, canvas.width, canvas.height);
            var off = 5;
            var ii=0;
            for (ii=0;ii<data.bars.length;ii++) {
                if ((ii % 2) == 0) {
                    cntx.fillRect(off, 0, data.bars[ii], canvas.height);
                }
                off += data.bars[ii];
            }
        }
    });
});

