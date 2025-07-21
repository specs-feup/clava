int main() {
	
	int a = 0;
	int b = 0;
	
	switch(a) {
		case 0:
			b = 1;
			b = 2;
			break;
		case 1 ... 3:
			b = 2;
			break;
        case 4:
			/* empty case with a text element */
			break;			
		default: /* text element */
			b = 3;
	}
	
	
	switch(b) {
		case 5:
		case 6:
		case 7:
		    /* Comment */
			b = 7;
			break;		
		case 8:
		case 9:
			b = 9;
			break;
		case 10: {
			goto label_a;
			}
			break;					
		case 11:
		label_a:
		case 12:	
			break;		
		
	}
}