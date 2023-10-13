root= / #[YOUR LOCAL PATH to owl files directory]

for file in $root/LUBM-DL/Base_1000U/*.owl
do
 echo $file
 echo $root/"$(basename "$file" .owl).dlp"
 java -jar loadDL/lib/owl2dlgp-1.1.0.jar -f $file -o $root/DLP/Base_1000U/"$(basename "$file" .nt).dlp"

done
