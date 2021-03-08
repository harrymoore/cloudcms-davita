/*
 * Author: Sławomir Netteria.NET https://netteria.net
 */
(function ($) {

    $.fn.VideoPopUp = function (options) {
        var element = this;
        
        var defaults = {
            backgroundColor: "#000000",
            opener: "video",
            maxweight: "640",
            pausevideo: false,
            idvideo: "",
            container: "videCont"
        };
        
        // var element = this.attr('id');
        var settings = $.extend({}, defaults, options);
        var video = $('#' + settings.idvideo);
        var container = $('#' + settings.container);
        var opener = $('#' + settings.opener);

        function stopVideo() {
            var tag = $(element).get(0).tagName;
            if (tag == 'video') {
                video.pause();
                video.currentTime = 0;
            }
        }
        
        $(element).css("position", "absolute")
        $(element).css("top", "0");
        $(element).css("left", "0");
        $(element).css("height", "100%")
        $(element).css("width", "100%")
        $(element).css("display", "none");
        $(element).css("justify-content", "center")
        $(element).css("alight-items", "center")
        // $(element).css("background", "#000");
        $(element).css("background", "rgba(0,0,0,0.5)");
        $(element).css("vertical-align", "vertical-align");
        $(element).css("padding", "25px");
        $(element).css("z-index", "10000");
        // $(container).css("z-index", "400005");

        // $(element).append('<div id="opct"></div>');
        // $(element).find('#opct').css("background", settings.backgroundColor);
        // $(element).css("z-index", "200001");
        // $(element).css("display", "flex")
        // $(element).css("bottom", "0");
        // $(element).css("right", "0");
        // $(element).css("padding", "auto");
        // $(element).css("text-align", "center");
        $(element).append('<div id="closer_videopopup">&otimes;</div>');
        $(opener).on('click', function () {
            $(element).show();
            $(video).trigger('play');

        });
        $(element).find("#closer_videopopup").on('click', function () {
            if(settings.pausevideo==true){
                $(video).trigger('pause');
            } else {
                stopVideo();
            }
            $(element).hide();
        });

        return this.css({});
    };

}(jQuery));
