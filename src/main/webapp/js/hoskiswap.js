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
  var hoskiswapshiftservice = $("meta[name='hoskiswapshifts']").attr("content");
  var hoskiswapshifts = $("#hoskiswapshifts");

  hoskiswapshifts.load(hoskiswapshiftservice, function() {
    hoskiswapshifts.removeClass("ajaxload");
  });

  var hoskiswapcalendarservice = $("meta[name='hoskiswapcalendar']").attr("content");
  var hoskiswapcalendar = $("#hoskiswapcalendar");

  hoskiswapcalendar.load(hoskiswapcalendarservice, function() {
    hoskiswapcalendar.removeClass("ajaxload");
  });
  
  var hoskiswappendingservice = $("meta[name='hoskiswappending']").attr("content");
  var hoskiswappending = $("#hoskiswappending");  

  function loadPending() {
    hoskiswappending.addClass("ajaxload");

    hoskiswappending.load(hoskiswappendingservice, function() {
      hoskiswappending.removeClass("ajaxload");
      hoskiswappending.find("form").submit(function() {
	var form = $(this);
	var submitButton = form.find("input[type='submit']");
	submitButton.attr("disabled", true).addClass("pending");
	$.post(form.attr("action"), form.serialize()).
	  always(loadPending);
	return false;
      });
    });
  } 

  loadPending();

  $("form.swap").submit(function() {
    var form = $(this);
    form.find(".error").hide();

    if (form.find("#hoskiswapterms:checked").size() == 0) {
      hoski.showMessage(form, null, "#eAcceptTerms");
    } else {
      var submitButton = form.find("input[type='submit']");
      submitButton.attr("disabled", true).addClass("pending");
      $.post(form.attr("action"), form.serialize())
	.fail(function(jqXHR, textStatus, errorThrown) {
	  hoski.showMessage(form, jqXHR, ".e500");
	  submitButton.attr("disabled", false).removeClass("pending");
	}).
	success(function(text, textStatus, jqXHR) {
          submitButton.hide();
	  loadPending();
	  if (!hoski.showMessage(form, jqXHR)) {
	    location.href = form.find("#nextURL").val()
	  }
	});
    }
    return false;
  });
});
