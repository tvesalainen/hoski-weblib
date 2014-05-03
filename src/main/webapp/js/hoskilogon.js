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
$(function () {
  var loginform = $("#hoskilogin");
  var logoutform = $("#hoskilogout");
  var activateform = $("#hoskiactivate");
  var activatedone = $("#hoskiactivatedone");
  var hoskiuser = $("#hoskiuser");
  var hoskiuserservice = $("meta[name='hoskiuser']").attr("content");
  
  function updateStatus() {
    $.getJSON(hoskiuserservice, function(data) {
      hoskiuser.removeClass("ajaxload");
      
      if (data && data.user) {
        hoskiuser.show().text(data.user["Jasenet.Email"]);

	populate($("body"), data.user);

        logoutform.show();
        loginform.hide();
        activateform.hide();
      } else {
	logoutform.hide();
        loginform.show().find("input[type='text']").focus();
        activateform.show();
      }
    });
  }

  function populate(root, data) {
    root.find("*[hoski\\:user]").each(function (j, elem) {
      elem = $(elem);
      elem.removeClass("ajaxload");
      var value = data[elem.attr("hoski:user")];
      elem.html(value);
    });

    root.find("*[hoski\\:userforeach]").each(function (j, elem) {
      elem = $(elem);
      var arr = data[elem.attr("hoski:userforeach")];
      while (arr && arr[0]) {
	var clone = elem.clone().insertBefore(elem).show();
	populate(clone, arr.shift());
      }
    });
  }
  
  function ajaxLogin() {
    loginform.find(".error").hide();
    var submitButton = loginform.find("input[type='submit']");
    submitButton.attr("disabled", true).addClass("pending");
    $.post(loginform.attr("action"), loginform.serialize())
      .done(function() {
	location.reload();
      })
      .fail(function(jqXHR, textStatus, errorThrown) {
        if (jqXHR.status === 403) {
          loginform.find(".e403").show();
        } else {
          loginform.find(".e500").show();
        }
        submitButton.attr("disabled", false).removeClass("pending");
      });
    return false;
  }

  function ajaxActivate() {
    activateform.find(".error").hide();

    var submitButton = activateform.find("input[type='submit']");
    submitButton.attr("disabled", true).addClass("pending");
    $.post(activateform.attr("action"), activateform.serialize())
      .done(function() {
        activateform.hide();
        activatedone.show();
      })
      .fail(function(jqXHR, textStatus, errorThrown) {
        if (jqXHR.status === 400 || jqXHR.status === 403) {
          activateform.find(".e403").show();
        } else {
          activateform.find(".e500").show().append("<p>" + errorThrown + "</p>");
        }
      })
      .always(function() {
        submitButton.attr("disabled", false).removeClass("pending");
      });

    return false;
  }

  function ajaxLogout() {
    $.post(logoutform.attr("action"))
      .always(function() {
	location.reload();
      });
    return false;
  }

  activateform.submit(ajaxActivate);
  loginform.submit(ajaxLogin);
  logoutform.submit(ajaxLogout);

  updateStatus();
});
