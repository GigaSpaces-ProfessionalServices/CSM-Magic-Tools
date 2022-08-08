var hostName;
var reloadInterval = '30000';
window.setInterval('reloadWindow()','3000000');

function reloadWindow(){
    window.location.href = window.location.href;
}
window.onload = function(){
    hostName = location.host;
    var errDiv = document.getElementById("errorMsg");
    var elms = document.querySelectorAll("[id='inError']");
    if(elms.length > 0){
        document.getElementById("totalInError").innerHTML = elms.length;
    }

    if(errDiv==null){
        var ele = document.getElementById("NoData");

        if(ele!=null){

            setTimeout('submitForm()','5000'); // Submit form only once to load initial data
        }
    }
}

function submitForm(){
    document.getElementById("form").submit();
}

function resetButtonLabel(id){
    //alert("resetButtonLabel"+id)
    $("#"+id).html("Try it!");
}
function tryService(url,id){
    $.ajax({
            type : "GET",
            url : url,
            success : function(result) {
                if(result=="success"){
                    $("#"+id).html("OK");
                } else{
                    alert("Request Failed!");
                }

                setTimeout('resetButtonLabel("'+id+'")',30000);
            },
            error : function(e) {

                console.log("ERROR: ", e);
            }
        });
    return false;


}
