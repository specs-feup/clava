function update_doc(doc_link) {
	//document.getElementById("doc_frame").src=doc_link; 
	//document.getElementById("doc_content").innerHTML='<object type="text/html" data="' + doc_link +  '" ></object>';
	$.ajax({
            url : doc_link,
            dataType: "text",
            success : function (data) {
                $("#doc_content").html(data);
            }
        });
}

//When the user scrolls down 20px from the top of the document, show the button
window.onscroll = function() {scrollFunction()};

function scrollFunction() {
    if (document.body.scrollTop > 20 || document.documentElement.scrollTop > 20) {
        document.getElementById("topButton").style.display = "block";
    } else {
        document.getElementById("topButton").style.display = "none";
    }
}

// When the user clicks on the button, scroll to the top of the document
function goToTop() {
    document.body.scrollTop = 0;
    document.documentElement.scrollTop = 0;
}
