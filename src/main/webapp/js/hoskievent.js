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
  var dialog = $("#admin-dialog").dialog({autoOpen: false});
  var sailWaveDialog = $("#admin-dialog-sailwave").dialog({autoOpen: false});
  var removeAttachmentDialog = $("#admin-dialog-remove-attachment").dialog({autoOpen: false});
  var addAttachmentDialog = $("#admin-dialog-add-attachment").dialog({autoOpen: false});

  $(".hoski-year").load("/admin", {'action': 'year'});
  $(".logout").hide();
  $(".login").show();
      
  $(".load").each(function(i, tag)
  {
    $(this).load($(this).attr("title"), function() {
      $(this).removeClass("ajaxload");
      $(this).removeAttr("title");
    $("[data-hoski-key]").mousedown(function(e){
        adminEvent(e, this);
    });
    $("[data-hoski-key]").each(function(e){
        $(".logout").show();
        $(".login").hide();
        return false;
    });
    });
  });
    
  var hoskievents = $("#hoskievents");
  var hoskieventservice = $("meta[name='hoskievents']").attr("content");

  hoskievents.load(hoskieventservice, function() {
    hoskievents.removeClass("ajaxload");
    $("[data-hoski-key]").mousedown(function(e){
        adminEvent(e, this);
    });
    $("[data-hoski-key]").each(function(e){
        $(".logout").show();
        $(".login").hide();
        return false;
    });
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
      location.search.replace("?", tmp.indexOf("?") !== -1 ? "&" : "?");
    hoskireservationslink.attr("href", hoskireservationservice);
    hoskireservations.load(hoskireservationservice + " tbody > *", function() {
      hoskireservations.removeClass("ajaxload");
      hoskireservationstotal.removeClass("ajaxload").text(hoskireservations.find("tr").size());
      hoskireservations.closest("table").tablesorter();
    });
  }

function adminEvent(e, el){
    if (e.which === 3){
        var key = $(el).attr("data-hoski-key");
        $.post("/admin", {'action': 'auth'}, function(res){
            dialog.dialog("open");
            $("#admin-dialog-sailwave-button").one("click", function(){
                $("#admin-dialog-sailwave").load("/admin", {'action': 'download-sailwave-dialog', 'key': key}, function(){
                    sailWaveDialog.dialog("open");
                    dialog.dialog("close");
                    $(this).on('submit', function(){
                        sailWaveDialog.dialog("close");
                    });
                });
            });
            $("#admin-dialog-remove-attachment-button").one("click", function(){
                $("#admin-dialog-remove-attachment").load("/admin", {'action': 'remove-attachment-dialog', 'key': key}, function(){
                    removeAttachmentDialog.dialog("open");
                    dialog.dialog("close");
                    $(this).on('submit', function(){
                        removeAttachmentDialog.dialog("close");
                    });
                });
            });
            $("#admin-dialog-add-attachment-button").one("click", function(){
                $("#admin-dialog-add-attachment").load("/admin", {'action': 'add-attachment-dialog', 'key': key}, function(){
                    addAttachmentDialog.dialog("open");
                    dialog.dialog("close");
                    $(this).on('submit', function(){
                        addAttachmentDialog.dialog("close");
                    });
                    $("#chooseFile").on("click", function(){
                        $("#attachmentFile").click();
                        $("#attachmentFile").on("change", function(){
                            var filename = $(this).val();
                            var idx = filename.lastIndexOf('\\');
                            if (idx === -1){
                                idx = filename.lastIndexOf('/');
                            }
                            if (idx !== -1){
                                filename = filename.slice(idx+1);
                            }
                            $(this).attr('name', filename);
                            $("#attachmentFilename").val(filename);
                        });
                    });
                });
            });
        });
    }
}
});


