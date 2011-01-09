<?php echo '<?xml version="1.0" encoding="UTF-8"?>'."\n"; ?>
<rss version="2.0">

<channel>
<title>SendToServer Feed</title>
<link><?php echo "http://".$_SERVER['HTTP_HOST'].$_SERVER['REQUEST_URI']; ?></link>
<description>Feed of all uploaded links and files</description>
<language>en</language>
<lastBuildDate><?php echo date('D, d M Y H:i:s O'); ?></lastBuildDate>
<?php 
$maxstrlen = 50;
$filepath = '/webdav/';
$files = array();
$folders = array();
if ($handle = opendir('.'.$filepath)) {
    while (false !== ($file = readdir($handle))) {
        if (preg_match('/URL.*\.txt/', $file)) {
						$files[] = $file;
				} else if (preg_match('/^Image-\d{14}$/', $file)) {
						$folders[] = $file;
				}
		}
		closedir($handle);
}
sort($files);
foreach ($files as $file) {
	echo "\n".'<item>'."\n";
	$match = '';
	$link = '';
	$title = '';
	$date = array();
	$content = file_get_contents(".$filepath".$file);
	if (preg_match('/^[a-zA-Z]+[:\/\/]+[A-Za-z0-9\-_]+\\.+[A-Za-z0-9\.\/%&=\?\-_]+$/i', $content, $match)) {
		$title = $match[0];
		$link  = $match[0];
	} else {
		$lines = preg_split('/\n/', $content);
		$title = (strlen($lines[0]) < $maxstrlen) ? $lines[0] : substr($lines[0],0 ,$maxstrlen);
		$link = 'https://'.$_SERVER['HTTP_HOST'].$filepath.$file;
	} 
	preg_match('/URL-(\d{4})(\d{2})(\d{2})(\d{2})(\d{2})(\d{2})/', $file, $date);
	echo '<title>'.htmlspecialchars(trim($title)).'</title>'."\n";
  echo '<link>'.htmlspecialchars($link).'</link>'."\n";
	echo '<gid>'.substr($file,0,-4).'</gid>'."\n";
	echo '<pubDate>'.date('D, d M Y H:i:s O', mktime($date[4], $date[5], $date[6], $date[2], $date[3], $date[1])).'</pubDate>'."\n";
	echo '</item>'."\n";
}
?>

</channel>
<?php //print_r($folders); ?>
</rss>
