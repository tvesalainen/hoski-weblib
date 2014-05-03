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
  function getParameterByName(name) {
    var match = RegExp('[?&]' + name + '=([^&]*)')
      .exec(window.location.search);
    
    return match && decodeURIComponent(match[1].replace(/\+/g, ' '));
  }

  var hoskiuserservice = $("meta[name='hoskiuser']").attr("content");
  var hoskiform = $(".hoskiform");
  var event = getParameterByName("ancestor");

  hoskiform.each(function(i, form) {
    form = $(form);    
    if (event && form.hasClass("event")) {
      form.find("input[name='event']").attr("value", event);
    }
  });

  hoskiform.submit(function() {
    var form = $(this);
    form.find(".error").hide();
    var submitButton = form.find("input[type='submit']");
    submitButton.attr("disabled", true).addClass("pending");
    $.post(form.attr("action"), form.serialize())
      .fail(function(jqXHR, textStatus, errorThrown) {
	hoski.showMessage(form, jqXHR, ".e500");
	submitButton.attr("disabled", false).removeClass("pending");
      }).
      success(function(text, textStatus, jqXHR) {
        submitButton.hide();
	if (!hoski.showMessage(form, jqXHR)) {
	  location.href = form.find("#nextURL").val()
	}
      });
    return false;
  });

  function populate(root, data) {

    root.find("input[type='text'][class~='hoskiform']").each(function(j, input) {
      var value = data[input.name];
      if (value) {
        input.value = value;
      } else {
        input.value = "";
      }
    });
    // hidden fields are not reset!
    root.find("input[type='hidden']").each(function(j, input) {
      var value = data[input.name];
      if (value) {
        input.value = value;
      }
    });
    // checkbox and radiobutton
    root.find("input[type='checkbox'], input[type='radio']").each(function(j, input) {
      if (input.value === data[input.name]) {
        input.checked = true;
      }
    });
    // magic areas
    root.find("*[hoski\\:if]").each(function(i, elem) {
      elem = $(elem);
      if (data[elem.attr("hoski:if")]) {
	elem.show();
      } else {
	elem.hide();
      }
    });
    // selects
    root.find("select").each(function(i, select) {
      var name = select.name;
      select = $(select);
      
      var options = select.attr("hoski:collection");
      if (options && data[options]) {
        $.each(data[options], function(i, option) {
	  var opt = $("<option/>", {
            value: option[select.attr("name")],
            text: option[select.attr("hoski:label")]
	  });
	  opt.data("data", option);
          select.append(opt);
        });
	root.find("*[hoski\\:on='" + select.attr('id') +"']").each(function(i, elem) {
	  elem = $(elem);
	  select.change(function() {
	    populate(elem, select.find("option:selected").data("data"));
	  }).change();
	});
      }

      if (data[name]) {
	select.val(data[name]);
      }
    });
  }

  $.getJSON(hoskiuserservice, function(data) {
    if (data && data.user) {
      hoskiform.each(function(i, form) {
        form = $(form);
	form.find("input[type='text'][class~='hoskiform']").removeClass("ajaxload");
	populate(form, data.user);
      });
    }
  });
});

