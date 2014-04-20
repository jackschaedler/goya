var supportsColorInput = (function() {
    var inputElem = document.createElement('input'), bool, docElement = document.documentElement, smile = ':)';

    inputElem.setAttribute('type', 'color');
    bool = inputElem.type !== 'text';

    if (bool) {

        inputElem.value         = smile;
        inputElem.style.cssText = 'position:absolute;visibility:hidden;';

        // chuck into DOM and force reflow for Opera bug in 11.00
        // github.com/Modernizr/Modernizr/issues#issue/159
        docElement.appendChild(inputElem);
        docElement.offsetWidth;
        bool = inputElem.value != smile;
        docElement.removeChild(inputElem);
    }

    return bool;
})();


function get_browser(){
    var N=navigator.appName, ua=navigator.userAgent, tem;
    var M=ua.match(/(opera|chrome|safari|firefox|msie)\/?\s*(\.?\d+(\.\d+)*)/i);
    if(M && (tem= ua.match(/version\/([\.\d]+)/i))!= null) M[2]= tem[1];
    M=M? [M[1], M[2]]: [N, navigator.appVersion, '-?'];
    return M[0];
    }

function get_browser_version(){
    var N=navigator.appName, ua=navigator.userAgent, tem;
    var M=ua.match(/(opera|chrome|safari|firefox|msie)\/?\s*(\.?\d+(\.\d+)*)/i);
    if(M && (tem= ua.match(/version\/([\.\d]+)/i))!= null) M[2]= tem[1];
    M=M? [M[1], M[2]]: [N, navigator.appVersion, '-?'];
    return M[1];
    }

var browser = get_browser();
var browser_version = get_browser_version();
var browser_major_version = parseInt(browser_version.split(".")[0]);

if(supportsColorInput) {
   document.getElementById("browser-blocker").style.visibility = "hidden";
} else {
   document.getElementById("browser-info").innerHTML = browser + " version: " + browser_major_version;
   document.getElementById("browser-blocker").style.visibility = "visible";
}
