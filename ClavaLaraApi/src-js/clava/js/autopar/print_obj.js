/**************************************************************
* 
*                       print_obj
* 
**************************************************************/
function print_obj(obj,msg,tap)
{

	msg = (typeof msg !== 'undefined') ? msg : '';
	tap = (typeof tap !== 'undefined') ? tap : '\t';
		   
    var strobj = ObjToStr(obj);
    for (var i = 0; i < strobj.length; i++)    
    {
    	str = strobj[i];
    }

}
/**************************************************************
* 
*                       ObjToStr
* 
**************************************************************/
function ObjToStr(obj)
{   
    var strobj = [];

    for (var i = 0; i < obj.length; i++)
    {
        var str = '';
        var myObject = obj[i];
        var tmpstr = Object.prototype.toString.call(myObject);
        
        if (tmpstr.indexOf('pt.up.fe.specs') !== -1 )
        {
                strobj.push(myObject.code);
                continue;
        }
        
        var keys = Object.keys(myObject);
        for (var j = 0; j < keys.length; j++)
        {
            key = keys[j];
            if (key === 'astId') continue;
            if (typeof myObject[key] === 'object' && myObject[key] !== null)
                str += '';
            else
                str += key + ' : ' + myObject[key] +' \t';
                
        }
        strobj.push(str);
    }
    return strobj;
}