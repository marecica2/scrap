var scrap = {};
scrap.services = {};
scrap.isProfile = false;
scrap.isAdmin = typeof sadm != "undefined" ? true : false;

scrap.initRating = function(){
    $(document).ready(function(){
        var stars = $(".rating");
        var rating = $("#rating").val();
        for(var i = 0; i < rating; i++){
            $(stars[i]).children().first().addClass("icon-star");
            $(stars[i]).children().first().removeClass("icon-star-empty");
        }
        
        for(var i = 4; i > (rating-1); i--){
            $(stars[i]).children().first().removeClass("icon-star");
            $(stars[i]).children().first().addClass("icon-star-empty");
        }
        
        
        $(".rating").click(function(){
            event.preventDefault();
            var rating = $(this).attr("data-rating");
            $("#rating").val(rating);
        });
        
        $(".rating").hover(function(){
            var rating = $(this).attr("data-rating");
            $("#rating").val(rating);
            var stars = $(".rating");
            for(var i = 0; i < rating; i++){
                $(stars[i]).children().first().addClass("icon-star");
                $(stars[i]).children().first().removeClass("icon-star-empty");
            }
            
            for(var i = 4; i > (rating-1); i--){
                $(stars[i]).children().first().removeClass("icon-star");
                $(stars[i]).children().first().addClass("icon-star-empty");
            }
        });

    });
}

scrap.searchAds = function(){
    var params = "first="+scrap.first+"&count="+scrap.count;
    scrap.getAds($("#advert-container"), params);    
};

scrap.initAds = function(isProfile){
    scrap.isProfile = isProfile;
    
    $(document).ready(function(){
        var params = scrap.utils.getHashParams();
        scrap.hasMore = true;
        scrap.first = 0;
        //scrap.count = 2;
        scrap.count = params.first != undefined? parseInt(params.first) :  10;
        
        var params = "first="+scrap.first+"&count="+scrap.count;
        if(isProfile != undefined)
            params += "&profile=true";
        
        scrap.getAds($("#advert-container"), params, function(){
            
        });

        $("#search-button").click(function(){
            event.preventDefault();
            console.log("sasas");
            
            scrap.hasMore = true;
            scrap.first = 0;
            scrap.count = 2;
            $("#search-form").submit();
        });

        // event handlers
        $("#moreResults").click(function(){
            if(scrap.hasMore){
                scrap.first += scrap.count;
                params = "first="+scrap.first+"&count="+scrap.count;
                scrap.appendAds($("#advert-container"), params);
            } else {
            }
            event.preventDefault();
        });
        
        $(window).scroll(function() {
            //console.log($(window).scrollTop() + "  " + ($(document).height() - $(window).height()))
            if (($(window).scrollTop()) == ($(document).height() - $(window).height()) ) {
                if(scrap.hasMore){
                    scrap.first += scrap.count;
                    var params = "first="+scrap.first+"&count="+scrap.count;
                    location.hash = params;
                    scrap.appendAds($("#advert-container"), params);
                } else {
                    //console.log("no more results");
                }
            }
       }); 
        
    });    
};

scrap.services.getAds = function(params, success, error){
    var endpoint = "/ads";
    if(scrap.isProfile)
        endpoint = "/user-ads"
    
    var query = location.search;
    if(query == "")
        query = "?query=user";

    $.ajax({
        type: "GET",
        url: endpoint+query+"&"+params,
        success: success,
        error: error,
        contentType: "application/json"
    });
};

scrap.getAds = function(elm, params, clbck){
    elm.html("");
    $("#showMoreContainer").hide();
    $("#spinner").show();
    scrap.services.getAds(params, function(data){
        var html = "";
        html += "<div id='dsidx' class='dsidx-results'>";
        html += "<ol id='dsidx-listingss' style='margin-left:0px;'>";
        for(var i = 0; i < data.length; i++){
            html += scrap.convertToHtml(data[i]);
        }
        html += "</ol>";
        html += "</div>";
        
        $(html).hide().appendTo(elm).fadeIn(1000);
        $("#spinner").hide();
        $("#showMoreContainer").show();
        
        if(clbck != undefined)
            clbck();
    });
};

scrap.appendAds = function(elm, params, clbck){
    $("#spinner").show();
    scrap.services.getAds(params, function(data){
        var html = "";
        if(data.length == 0)
            scrap.hasMore = false;
        for(var i = 0; i < data.length; i++){
            html += scrap.convertToHtml(data[i]);
        }
        $(html).hide().appendTo(elm).fadeIn(1000);
        $("#spinner").hide();
    });
};

scrap.convertToHtml = function(data){
    var html ="";
    html += "<div class='box-container'>";
    html += "  <div class='holder row'>";
    html += "    <a class='" + (scrap.isProfile?"span3":"span3") + " overlay' href='/ad?uuid="+data.uuid+"' title='"+data.uuid+"'>";
    html += "      <span class='more'>";
    html += "      </span>";
    if(data.fileUploads.length > 0){
    html += "      <span style='border-top:7px solid #050505;border-left:5px solid #050505;margin:0px;width:120px;position:absolute;right:0px;bottom:0px'><img src='public/uploads/"+data.fileUploads[0].url+"_thumb'/>";
    html += "      </span>";
    }
    if(data.type == "001")
        html += "      <img src='public/images/type"+data.type+data.category+".jpg' class='attachment-property-regular wp-post-image'></a>";
    else
        html += "      <img src='public/images/type"+data.type+".jpg' class='attachment-property-regular wp-post-image'></a>";
    
    var label = i18n("ad.type."+data.type);
    
    if(data.category != "" && data.category != undefined)
        label = i18n("ad.category."+data.category);

    html += "    <span class='prop-tag'>"+label+" ";
    html += "    </span>";
    
    
    html += "    <div class='"+(scrap.isProfile?"span6":"span5")+" prop-info'>";
    if(scrap.isProfile){
        html += "      <span class='prop-title'>"+i18n("ad.pricetype."+data.priceType) + ", "+ i18n("ad."+data.buySell)+"</span>";
        html += "       <a href='/ad?uuid="+data.uuid+"' style='margin-left:10px;color:black' class='pull-right'><i class='icon-search'></i></a>";
        html += "       <a href='/ad-edit?uuid="+data.uuid+"' style='margin-left:10px;color:black' class='pull-right'><i class='icon-pencil'></i></a>";
        html += "      </span>";
    } else {
        html += "      <span class='prop-title'>"+i18n("ad.pricetype."+data.priceType) + ", "+ i18n("ad."+data.buySell)+"</span>";
    }
    
    if(scrap.isProfile){
        var color = data.state == "001" ? "important" : "success";
        if(data.state != "001" && data.expired){
            data.state = "003";
            color = "important"
        }
        html += "       <span class='pull-right label label-"+color+"'>"+i18n("ad.state."+data.state)+"</span>";
    }

    html += "      </br>";
    html += "      <ul class='more-info clearfix'>";
    html += "        <li class='info-label clearfix'>";
    html += "          <span class='pull-left'><i class='icon-plus'></i> "+i18n("ad.type");
    html += "          </span>";
    html += "          <span class='qty pull-right'>"+i18n("ad.type."+data.type);
    html += "          </span>";
    html += "        </li> ";
    
    if(data.type == "001"){
        html += "        <li class='info-label clearfix'>";
        html += "          <span class='pull-left'><i class='icon-tag'></i> "+i18n("ad.category");
        html += "          </span>";
        html += "          <span class='qty pull-right'>"+i18n("ad.category."+data.category);
        html += "          </span>";
        html += "        </li> ";
        if(+data.subCategory != null){
            html += "        <li class='info-label clearfix'>";
            html += "          <span class='pull-left'><i class='icon-tags'></i> "+i18n("ad.subcategory");
            html += "          </span>";
            html += "          <span class='qty pull-right'>"+i18n("ad.subcategory."+data.subCategory);
            html += "          </span>";
            html += "        </li> ";
        }
    }
    
    html += "        <li class='info-label clearfix'>";
    html += "          <span class='pull-left'><i class='icon-calendar'></i> "+i18n("ad.published");
    html += "          </span>"; 
    if(data.published != null){
        html += "          <span class='qty pull-right'>"+scrap.format(data.published);
        html += "          </span>";
    }
    html += "        </li> ";
    html += "        <li class='info-label clearfix'>";
    html += "          <span class='pull-left'><i class='icon-map-marker'></i> "+i18n("ad.location");
    html += "          </span>";
    html += "          <span class='qty pull-right'>";
    html += "          "+i18n("country."+data.locationCountry);
    html += "          </span>";
    html += "        </li>";
    if(data.type == "005" && data.distance != undefined && data.distance != null){
        
    html += "        <li class='info-label clearfix'>";
    html += "          <span class='pull-left'><i class='icon-map-marker'></i> "+i18n("ad.distance");
    html += "          </span>";
    html += "          <span class='qty pull-right'>";
    html += "          "+Number(data.distance).toFixed(2)+" km";
    html += "          </span>";
    html += "        </li>";
    }
    if(data.type == "005" || data.type == "006"){
    html += "        <li class='info-label clearfix'>";
    html += "          <span class='pull-left'><i class='icon-map-marker'></i> "+i18n("ad.description");
    html += "          </span>";
    html += "        </li>";
    html += "        <li class='info-label clearfix'>";
    html += "          <span class='qty '>";
    html += "          "+data.description;
    html += "          </span>";
    html += "        </li>";
    }
    html += "        <li class='info-label clearfix'>";
    html += "          <span class='pull-left'>";
    html += "          </span>";
    html += "          <span class='qty pull-right'>";
    html += "               <span class='label' style='padding:5px'><big><strong>" + data.amount + " kg</strong></big></span>";
    html += "               <span class='label label-inverse' style='padding:5px'><big><strong>" + data.price + " &euro;</strong></big></span>";
    html += "          </span>";
    html += "        </li> ";
    html += "      </ul>";
    html += "    </div>";
    html += "  </div>";
    html += "</div>";       
    return html;
};


scrap.uploadFiles = function(url, files, clbk) {
    for (var i = 0; i < files.length; i++) {
      var formData = new FormData();
      var file = files[i];
      formData.append("attachment", file);
      formData.append("contentType", file.type);
      formData.append("size", file.size);
      
      $("#progresses").append("<div id='progress"+i+"' class='progress progress-striped active' style='display:none'><div class='bar' id='bar"+i+"' style='width: 0%;'></div></div>");
      
      var xhr = new XMLHttpRequest();
      xhr.open('POST', url, true);
      
      (function(i, clbk) {
          var index = i;
          xhr.onload = function(e) { 
              $("#upload").replaceWith($("#upload").clone(true));                  
              $("#progress"+index).remove();
              var uuid = e.currentTarget.responseText;
              
              if(clbk != undefined)
                  clbk(uuid);
              
              //console.log('upload successful ' + index); 
              $.post("/delete-temp-files", function(){});
          };
          
          xhr.upload.onprogress = function(e) {
              if (e.lengthComputable) {
                  var up = (e.loaded / e.total) * 100;
                  $("#bar"+index).css("width", up+"%");
              }
          }
      })(i, clbk);      

      $("#progress"+i).show();
      $("#bar"+i).html(file.name);
      xhr.send(formData);
    }
};       





// chat
scrap.chat = false;
$(document).ready(function(){
    setInterval(function(){
        if(scrap.chat){
            $.get("/my-chats", function(data){
                var html = "";
                var online = true;
                for(var i = 0; i < data.length; i++){
                    if(data[i].inverted) {
                        html += "<b><a class='chat-user-switch' data-uuid='"+data[i].adminUuid+"' href='#'>"+data[i].adminName +"</a></b>";
                        if(scrap.isAdmin){
                            html += " &raquo; <b><a class='chat-user-switch' data-uuid='"+data[i].userUuid+"' href='#'>"+data[i].name +"</a></b>";
                        }
                        html += "&nbsp;<small style='color:silver'>" + data[i].created +"</small></br>";
                        html += data[i].text+"<br/>";
                    } else {
                        html += "<b><a class='chat-user-switch' data-uuid='"+data[i].userUuid+"' href='#'>"+data[i].name +"</a></b>";
                        html += "&nbsp;<small style='color:silver'>" + data[i].created +"</small></br>";
                        html += data[i].text+"<br/>";
                    }
                    if(!data[i].adminOnline)
                        online = false;
                }
                
                if(!scrap.isAdmin){
                    if(online) {
                        $("#chat-container").html(html);           
                        $("#chat-text").show();
                        $("#chat-send").show();
                    }
                    else {
                        $("#chat-container").html(i18n("admin.offline"));           
                        $("#chat-text").hide();
                        $("#chat-send").hide();
                    }
                } else {
                    $("#chat-container").html(html);           
                }
            });
        }
    }, 3000);
    
    if(scrap.isAdmin){
        $.get("/users", function(data){
            var html = "<option value=''></option>";
            for(var i = 0; i < data.length; i++){
                html += "<option value='"+data[i].uuid+"'>"+data[i].name+"</option>";
            }
            $("#chat-to").html(html);
        });
    }
    
    scrap.sendChat = function(){
        if($("#chat-text").val() != ""){
            $.post("/my-chats-post", {message: $("#chat-text").val(), to: $("#chat-to").val() }, function(data){
                $("#chat-text").val("");
            });
        }
    }
    
    $("#chat-container").on("click", ".chat-user-switch", function(){
        $("#chat-to").val($(this).attr("data-uuid"));
    });
    
    $("#chat-send").click(function(){
        scrap.sendChat();
    });
    
    $('#chat-text').bind('keypress', function(e) {
        if(e.keyCode==13){
            scrap.sendChat();
        }
    });
});        




scrap.utils = {};

scrap.utils.mobilecheck = function() {
    var check = false;
    (function(a){if(/(android|bb\d+|meego).+mobile|avantgo|bada\/|blackberry|blazer|compal|elaine|fennec|hiptop|iemobile|ip(hone|od)|iris|kindle|lge |maemo|midp|mmp|mobile.+firefox|netfront|opera m(ob|in)i|palm( os)?|phone|p(ixi|re)\/|plucker|pocket|psp|series(4|6)0|symbian|treo|up\.(browser|link)|vodafone|wap|windows (ce|phone)|xda|xiino/i.test(a)||/1207|6310|6590|3gso|4thp|50[1-6]i|770s|802s|a wa|abac|ac(er|oo|s\-)|ai(ko|rn)|al(av|ca|co)|amoi|an(ex|ny|yw)|aptu|ar(ch|go)|as(te|us)|attw|au(di|\-m|r |s )|avan|be(ck|ll|nq)|bi(lb|rd)|bl(ac|az)|br(e|v)w|bumb|bw\-(n|u)|c55\/|capi|ccwa|cdm\-|cell|chtm|cldc|cmd\-|co(mp|nd)|craw|da(it|ll|ng)|dbte|dc\-s|devi|dica|dmob|do(c|p)o|ds(12|\-d)|el(49|ai)|em(l2|ul)|er(ic|k0)|esl8|ez([4-7]0|os|wa|ze)|fetc|fly(\-|_)|g1 u|g560|gene|gf\-5|g\-mo|go(\.w|od)|gr(ad|un)|haie|hcit|hd\-(m|p|t)|hei\-|hi(pt|ta)|hp( i|ip)|hs\-c|ht(c(\-| |_|a|g|p|s|t)|tp)|hu(aw|tc)|i\-(20|go|ma)|i230|iac( |\-|\/)|ibro|idea|ig01|ikom|im1k|inno|ipaq|iris|ja(t|v)a|jbro|jemu|jigs|kddi|keji|kgt( |\/)|klon|kpt |kwc\-|kyo(c|k)|le(no|xi)|lg( g|\/(k|l|u)|50|54|\-[a-w])|libw|lynx|m1\-w|m3ga|m50\/|ma(te|ui|xo)|mc(01|21|ca)|m\-cr|me(rc|ri)|mi(o8|oa|ts)|mmef|mo(01|02|bi|de|do|t(\-| |o|v)|zz)|mt(50|p1|v )|mwbp|mywa|n10[0-2]|n20[2-3]|n30(0|2)|n50(0|2|5)|n7(0(0|1)|10)|ne((c|m)\-|on|tf|wf|wg|wt)|nok(6|i)|nzph|o2im|op(ti|wv)|oran|owg1|p800|pan(a|d|t)|pdxg|pg(13|\-([1-8]|c))|phil|pire|pl(ay|uc)|pn\-2|po(ck|rt|se)|prox|psio|pt\-g|qa\-a|qc(07|12|21|32|60|\-[2-7]|i\-)|qtek|r380|r600|raks|rim9|ro(ve|zo)|s55\/|sa(ge|ma|mm|ms|ny|va)|sc(01|h\-|oo|p\-)|sdk\/|se(c(\-|0|1)|47|mc|nd|ri)|sgh\-|shar|sie(\-|m)|sk\-0|sl(45|id)|sm(al|ar|b3|it|t5)|so(ft|ny)|sp(01|h\-|v\-|v )|sy(01|mb)|t2(18|50)|t6(00|10|18)|ta(gt|lk)|tcl\-|tdg\-|tel(i|m)|tim\-|t\-mo|to(pl|sh)|ts(70|m\-|m3|m5)|tx\-9|up(\.b|g1|si)|utst|v400|v750|veri|vi(rg|te)|vk(40|5[0-3]|\-v)|vm40|voda|vulc|vx(52|53|60|61|70|80|81|83|85|98)|w3c(\-| )|webc|whit|wi(g |nc|nw)|wmlb|wonu|x700|yas\-|your|zeto|zte\-/i.test(a.substr(0,4)))check = true})(navigator.userAgent||navigator.vendor||window.opera);
    return check;
}

scrap.utils.getHashParams = function () {
    var hashParams = {};
    var e,
        a = /\+/g,  // Regex for replacing addition symbol with a space
        r = /([^&;=]+)=?([^&;]*)/g,
        d = function (s) { return decodeURIComponent(s.replace(a, " ")); },
        q = window.location.hash.substring(1);
    while (e = r.exec(q))
       hashParams[d(e[1])] = d(e[2]);
    return hashParams;
}

scrap.format = function(d){
    var date = new Date(d);
    var d = date.getDate();
    var m = date.getMonth() + 1;
    var y = date.getFullYear();
    var h = date.getHours();
    var min = date.getMinutes();
    var s = date.getSeconds();
    return d + "." + m + "." + y + " " + h + ":" + min;
}



