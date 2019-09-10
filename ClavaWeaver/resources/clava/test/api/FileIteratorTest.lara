import clava.util.FileIterator;
import lara.Io;
import clava.Clava;

aspectdef FileIteratorTest

	// Write source files to a temporary folder
	var tempFolder = Io.getTempFolder("ClavaFileIteratorTest");
	Clava.writeCode(tempFolder);

	// Iterate method 1
	var fileIterator = new FileIterator(tempFolder);
	
	var $file = fileIterator.next();
	while ($file !== undefined) {
		println("Iterator 1");
		$file = fileIterator.next();
	}
	
	// Iterate method 2
	var fileIterator2 = new FileIterator(tempFolder);

	while (fileIterator2.hasNext()) {
		var $file = fileIterator2.next();
		println("Iterator 2");
	}
	
	// Generic select after iterator
	var fileIterator3 = new FileIterator(tempFolder);
	fileIterator3.next();
	
	select file end
	apply
		println("Single file");
	end
	
	Io.deleteFolder(tempFolder);
	
end
