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
  $("#registrationbutton").click(function() {
    var e = $("div#registration").show();
    $(this).hide();
    var scrollTop = e.offset().top - 50;
    $('html,body').animate({scrollTop: scrollTop}, 500);
  });

  $("form.raceform").submit(function() {
    var form = $(this);
    form.find(".error").hide();

    // 1. check that mandatory fields have been filled
    var isOk = true;
    $(".required input:enabled").each(function(i, input) {
      var num = /^[0-9]+$/
      
      input = $(input);

      if (input.val() === "") {
	input.addClass("notOk").focus(function() { 
	  $(this).removeClass("notOk");
	});
	isOk = false;
      } else if (input.hasClass("numeric") && !num.test(input.val())) {
	input.addClass("notOk").focus(function() { 
	  $(this).removeClass("notOk");
	});
	isOk = false;
      }
    });

    if (!isOk) {
      $("#eRequiredFields").show();
    } else {
      var submitButton = form.find("input[type='submit'], button#submitbutton");
      submitButton.attr("disabled", true).addClass("pending");
      $.post(form.attr("action"), form.serialize())
	.fail(function(jqXHR, textStatus, errorThrown) {
	  hoski.showMessage(form, jqXHR, ".e500");
	  submitButton.attr("disabled", false).removeClass("pending");
	}).
	success(function(text, textStatus, jqXHR) {
          var entryKey = $(jqXHR.responseText).attr("id");
          location.href = "payment.html?ancestor="+entryKey;
          /*
          submitButton.hide();
	  if (!hoski.showMessage(form, jqXHR)) {
	    location.href = form.find("#nextURL").val()
	  }
          */
	});
    }

    return false;
  });
});

