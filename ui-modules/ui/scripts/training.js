define(function (require, exports, module) {
    require("css!./vendor/videopopup.css");
    require("./vendor/videopopup.js");
    const R = window.Ratchet;
    R.o = R.observable;

    const vidQuery = {
        "_type": "n:node", 
        "training-video": true, 
        "_fields": {
            "training-video-label": 1
        }
    };

    $(document).on('cloudcms-ready', function (ev) {
        if ($('div#training-video').length) {
            return;
        }
        
        var trainingVideos = [];
        var v1QueryUrl = `/proxy/repositories/${R.o("repository").get()._doc}/branches/${R.o("branch").get()._doc}/nodes/query?query=${encodeURIComponent(JSON.stringify(vidQuery))}&sort=${encodeURIComponent(JSON.stringify({order:1}))}&limit=10`;
        $.ajax({
            url: v1QueryUrl,
            success: function (result) {
                if (result.size && result.size > 0) {
                    trainingVideos = result.rows;
                }
            },
            async: false
        });

        if (!trainingVideos.length) {
            return;
        }

        var markup1 = "", markup2 = "";
        var n = 0;
        trainingVideos.forEach(v => {
            n += 1;
            markup1 += `
                <li><a href="javascript:void(0)" id="video-trigger${n}" class="actionlink help-docs">${v['training-video-label']}</a></li>
            `;

            markup2 += `
                <div id="training-video-vid-box${n}" class="training-video-vid-box">
                    <div id="video-container${n}">
                        <video id="training-video${n}" controls muted preload="none">
                            <source src="/proxy/repositories/${R.o("repository").get()._doc}/branches/${R.o("branch").get()._doc}/nodes/${v._doc}/attachments/default" type="video/mp4">
                            <!--
                            <source src="/preview/${v._doc}?repository=${R.o("repository").get()._doc}&branch=${R.o("branch").get()._doc}&node=${v._doc}&attachment=default&size=720&mimetype=video/mp4&name=trainingpreview" type="video/mp4">
                            -->
                        </video>
                    </div>
                </div>    
            `;
        });

        $('.btn-header-help').parent().before(`
            <div class="btn-group hidden-xs">
                <button type="button" class="btn btn-link  dropdown-toggle" data-toggle="dropdown" title="Training">
                    <i class="fa fa-question-circle"></i>&nbsp;Davita Training&nbsp;<span class="caret"></span>
                </button>

                <ul class="dropdown-menu pull-right" role="menu" style="z-index: 9999">
                    ${markup1}
                </ul>            
            </div>
        `);

        $('body').after(markup2);

        var n = 0;
        trainingVideos.forEach(v => {
            n += 1;
            $(`#training-video-vid-box${n}`).VideoPopUp({
                backgroundColor: "#17212a",
                opener: `video-trigger${n}`,
                maxweight: "720",
                idvideo: `training-video${n}`,
                container: `video-container${n}`
            });    
        });
    });
});
