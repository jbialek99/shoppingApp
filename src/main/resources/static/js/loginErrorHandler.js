// errorHandler.js

window.onload = function() {
    const url = new URL(window.location.href);
    if (url.searchParams.has('error')) {
        //usuwanie zbednego errorru
        url.searchParams.delete('error');
        // usuwanie error z adresu
        window.history.replaceState({}, document.title, url.toString());
    }
};
