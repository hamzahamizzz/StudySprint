<?php
$json = json_decode(file_get_contents('composer.lock'), true);
foreach ($json['packages'] as $package) {
    if (in_array($package['name'], ['doctrine/annotations', 'phpstan/phpdoc-parser'])) {
        echo $package['name'] . ': ' . $package['version'] . "\n";
    }
}
