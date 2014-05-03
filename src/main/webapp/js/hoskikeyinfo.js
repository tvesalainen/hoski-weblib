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
    
  var keyinfoservice = $("meta[name='keyinfo']").attr("content");
  var hoskiform = $(".hoskiform");
  
  $.getJSON(keyinfoservice, function(data) {
    if (data) {
      $("*[hoski\\:keyinfo]").each(function (j, elem) {
	elem = $(elem);
	elem.removeClass("ajaxload");
	var value = data[elem.attr("hoski:keyinfo")];
	if (elem.get(0).tagName.toLowerCase() === "canvas") {
	  drawBars(elem.get(0), value);
	} else if (value instanceof Array) {
	  elem.empty();
	  $.each(value, function(i, e) {
	    elem.append($("<li/>", {
              html: e
            }));
	  });
	} else {
	  elem.html(value);
	}
      });

      $("*[hoski\\:ifkeyinfo]").each(function(j, elem) {
	elem = $(elem);
	var cond = data[elem.attr("hoski:ifkeyinfo")];
	if (cond == true || cond == "true") {
	  // has the attribute and it is true
	  elem.show();
	} else {
	  elem.hide();
	}
      });

      $("*[hoski\\:unlesskeyinfo]").each(function(j, elem) {
	elem = $(elem);
	var cond = data[elem.attr("hoski:unlesskeyinfo")]
	if (!(cond == true || cond == "true")) {
	  // has the attribute and it is not true
	  elem.show();
	} else {
	  elem.hide();
	}
      });

      $("input[type='text'][class~='hoskikeyinfo'], input[type='hidden'][class~='hoskikeyinfo']").each(function(j, input) {
        if (input.value === "") {
          var value = data[input.name];
          if (value) {
            input.value = value;
          }
        }
      });      
    }
  });

  function drawBars(canvas, data) {
    var cntx = canvas.getContext("2d");
    cntx.clearRect(0, 0, canvas.width, canvas.height);
    var off = 0;
    for (var i = 0; i < data.length; i++) {
      if ((i % 2) == 0) {
	cntx.fillRect(off, 0, data[i], canvas.height);
      }
      off += data[i];
    }
  }
});

