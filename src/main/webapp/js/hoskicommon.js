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
var hoski = hoski || {}

hoski.showMessage = function(form, jqXHR, defaultMessage) {
  var e = $("");

  if (jqXHR) {
    var messageId = $(jqXHR.responseText).attr("id");
    var status = jqXHR.status;
    var e = form.find("#" + messageId).add(".e" + status);
  }
  
  if (defaultMessage) {
    e = e.add(defaultMessage);
  }

  if (e.first().show().size() > 0) {
    var scrollTop = e.offset().top - 50;
    $('html,body').animate({scrollTop: scrollTop}, 500);
    
    return true;
  } else {
    return false;
  }
  
}

$(function() {
  $("a.ajaxpopup").click(function() {
    var a = $(this);

    var popup = a.data("hoskipopup")

    if (!popup) {
      popup = $("<div><span class='ajaxload'>&nbsp;</span></div>");
      var href = a.attr("href");

      // reuse on all 'a's with same href
      $("a[href='" + href + "']").data("hoskipopup", popup);

      $("body").append(popup);
      popup.dialog({ autoOpen: false, 
		     width: Math.min(800, $("body").width()), 
		     modal: true, 
		     dialogClass: "hoskipopup" });
      popup.load(a.attr("href"));
    }

    popup.dialog('open');

    return false;
  });
});
