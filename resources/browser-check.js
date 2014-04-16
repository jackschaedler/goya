var isChromium = window.chrome,
    vendorName = window.navigator.vendor;
if(isChromium !== null && vendorName === "Google Inc.") {
   document.getElementById("browser-blocker").style.visibility = "hidden";
} else {
   document.getElementById("browser-blocker").style.visibility = "visible";
}
