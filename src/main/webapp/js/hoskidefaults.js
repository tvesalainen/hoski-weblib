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
  var hoskidefaultservice = $("meta[name='defaults']").attr("content");
  var raceform = $(".raceform");

  function populate(data) {
    if (data) {
      $("input[type='text']").each(function(j, input) {
        var value = data[input.name];
        if (value) {
          input.value = value;
        }
      });
      $("*[hoski\\:default]").each(function (j, elem) {
        elem = $(elem);
        elem.removeClass("ajaxload");
        var value = data[elem.attr("hoski:default")];
        elem.html(value);
      });
    }
  }

  function updateRating() {
    var params = {
      RatingSystem: $("#ratingsystem").val(),
      Nat: $("#nat").val(),
      SailNo: $("#sailno").val(),
      Class: $("#class").val()
    }

    $.getJSON(hoskidefaultservice, params, populate)
  }

  $.getJSON(hoskidefaultservice, populate)

  $("#nat, #sailno, #class").change(updateRating)
});



    

