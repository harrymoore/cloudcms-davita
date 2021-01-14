define(function(require, exports, module) {
    // var $ = require("jquery");

    // $(document).on('cloudcms-ready', function(ev) {
    //     let url = window.location.href;
    //     if (url && -1 !== url.indexOf('/content/davita:document/documents/') ) {
    //         $('.form-container .alpaca-field-text').css( "border", "2px solid #00ff00");
    //         $('.form-container .alpaca-field-text').each(el => {
    //             console.log(el);
    //         })
    //     }
    // });

    // use larger image previews
    document.addEventListener("load", function(ev) {
        var s = "" + ev.target.src;
        if (s && -1 !== s.indexOf('.cloudcms.net/preview/') && -1 !== s.indexOf('name=icon64') && -1 !== s.indexOf('size=64')) {
            s = s.replace("name=icon64", "name=iconbig");
            s = s.replace("size=64", "size=128");
            s = s.replace("mimetype=image/png", "mimetype=image/jpeg");
            ev.target.src = s;
        }
    }, true);

});