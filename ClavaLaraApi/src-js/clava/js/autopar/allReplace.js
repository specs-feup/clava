/**************************************************************
* 
*                       allReplace
* 
**************************************************************/
String.prototype.allReplace = function(obj) {
    var retStr = this;
    for (var x in obj) {
        retStr = retStr.replace(new RegExp(x, 'g'), obj[x]);
    }
    return retStr;
};


String.prototype.getBetweenBrackets = function()
{
	var matchlist = [];
	this.replace(/\[(.*?)\]/g, function(_, match){matchlist.push(match);});
	return matchlist;
}

String.prototype.moveBracketsToEnd = function(arraylist)
{
	var str = this;
	var index = 0;
	var matchlist = this;
	var i = 0;
	while(true)
	{
		posSTBracket = str.indexOf('[', index);		
		if (posSTBracket === -1)
			break;

		posSTspace = str.lastIndexOf(' ', posSTBracket)
		if (posSTspace === -1) posSTspace = 0;

		posFTBracket = str.indexOf(']', posSTBracket);
		posFTspace = str.indexOf(' ', posFTBracket);
		if (posFTspace === -1) posFTspace = str.length;

		var substr = str.substring(posSTspace, posFTspace);
		var betweenBracketslist = substr.getBetweenBrackets();
		var newsubstr = substr.replace(/\[.*?\]/g, "");


		arraylist.push(newsubstr + '(' + Array(betweenBracketslist.length+1).join('#').split('').join(',') +')');


		newsubstr = newsubstr + '(' + betweenBracketslist.join(',') + ')';
		matchlist = matchlist.replace(substr, newsubstr);
		index = posFTspace;
	}

	return matchlist;
}

function normalizeVarName(Varname)
{
	return Varname.replace(/\[.*?\]/g, "");
}

function normalizeVarName2(Varname)
{
	return Varname.replace(/\[.*?\]/g, "-");
}


String.prototype.moveBracketsToEnd3 = function(arraylistdic)
{


	if (this.indexOf('[') === -1)
		return this;

	var filteredstr = this.allReplace({'\t':' '}) + ' ';
	var outputstr = this;

	
	while(true)
	{
		if (outputstr.indexOf('[') === -1)
			break;

		var indexST = filteredstr.lastIndexOf(' ',filteredstr.indexOf('['));
		var indexFT = filteredstr.indexOf(']',indexST+1);
		while(indexFT < filteredstr.length)
		{
			if (
				filteredstr.indexOf('[',indexFT) !== -1
				&&
				filteredstr.substring(indexFT,filteredstr.indexOf('[',indexFT)).indexOf(' ') === -1
				)
				indexFT = filteredstr.indexOf(']',indexFT+1);
			else
				break;
		}
		indexFT = filteredstr.indexOf(' ', indexFT);

		var substr = filteredstr.substring(indexST+1,indexFT);
		var arrayName = normalizeVarName(substr);
		var betweenBracketslist = substr.getBetweenBrackets();
		
		var len = Object.keys(arraylistdic).length + 1;
		if 	(arraylistdic[arrayName] === undefined)
		{
			arraylistdic[arrayName] = {};
			arraylistdic[arrayName].name = 'A_' + len;
			arraylistdic[arrayName].size = '(0:9999' + Array(betweenBracketslist.length).join(',0:9999') +')';
		}
		var newsubstr = arraylistdic[arrayName].name + '(' + betweenBracketslist.join(',') +')';


		outputstr = outputstr.replace(substr, newsubstr);
		filteredstr = filteredstr.replace(substr, '');		
	}

	return outputstr;


}

