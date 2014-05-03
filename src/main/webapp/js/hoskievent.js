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
    
  $(".load").each(function(i, tag)
  {
    $(this).load($(this).attr("title"), function() {
      $(this).removeClass("ajaxload");
      $(this).removeAttr("title");
    });
  });
    
  var hoskievents = $("#hoskievents");
  var hoskieventservice = $("meta[name='hoskievents']").attr("content");

  hoskievents.load(hoskieventservice, function() {
    hoskievents.removeClass("ajaxload");
  });

  var hoskiweekly = $("#hoskiweekly");
  var hoskiweeklyservice = $("meta[name='hoskiweekly']").attr("content");

  hoskiweekly.load(hoskiweeklyservice, function() {
    hoskiweekly.removeClass("ajaxload");
  });

  var hoskiresults = $("#hoskiresults");
  var hoskiresultsservice = $("meta[name='hoskiresults']").attr("content");

  hoskiresults.load(hoskiresultsservice, function() {
    hoskiresults.removeClass("ajaxload");
  });

  var hoskireservations = $("#hoskireservations");
  var hoskireservationslink = $("a#hoskireservationslink");
  var hoskireservationstotal = $("#hoskireservationstotal");
  if (hoskireservations.size() > 0) {
    var tmp = $("meta[name='hoskireservations']").attr("content");
    var hoskireservationservice = tmp + 
      location.search.replace("?", tmp.indexOf("?") != -1 ? "&" : "?");
    hoskireservationslink.attr("href", hoskireservationservice);
    hoskireservations.load(hoskireservationservice + " tbody > *", function() {
      hoskireservations.removeClass("ajaxload");
      hoskireservationstotal.removeClass("ajaxload").text(hoskireservations.find("tr").size());
      hoskireservations.closest("table").tablesorter();
    });
  }
});

