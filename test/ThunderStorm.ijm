setBatchMode(true);
filelist = getArgument();
file = split(filelist,'#');

open(file[0]);
run("Camera setup", "offset=180.0 isemgain=true photons2adu=5.4 gainem=100.0 pixelsize=160.0");
run("Run analysis", "filter=[Difference of averaging filters] size1=3 size2=5 detector=[Local maximum] connectivity=8-neighbourhood threshold=std(Wave.F1) estimator=[PSF: Integrated Gaussian] sigma=1.6 fitradius=5 method=[Weighted Least squares] full_image_fitting=false mfaenabled=false renderer=[Averaged shifted histograms] magnification=5.0 colorizez=false threed=false shifts=2 repaint=50");

//get the rendered image
//f = File.open("IJLogDebug.txt");
//print(f,"DONE RUN\r\n")
print_win();
print("test123");
run("Export results", "filepath=t2.csv fileformat=[CSV (comma separated)] sigma=true intensity=true chi2=true offset=true saveprotocol=true x=true y=true bkgstd=true id=true uncertainty=true frame=true");

var rendered ="Averaged shifted histograms";
selectWindow(rendered);
saveAs("Tiff", rendered+".tiff");


//print(f,"Exported\r\n")

function print_win() {
	list = getList("image.titles");
	if (list.length==0)
	 print("No image windows are open");
	else {
	 print("Image windows:");
	 for (i=0; i<list.length; i++)
		print("   "+list[i]);
	}
	print("");

	list = getList("window.titles");
	if (list.length==0)
	 print("No non-image windows are open");
	else {
	 print("Non-image windows:");
	 for (i=0; i<list.length; i++)
		print("   "+list[i]);
	}
	print("");
}

//Save Logs:
//selectWindow("Log")
//saveAs("Text", "C:\\Logs\\ijLog.txt");
close();
//run("Quit");