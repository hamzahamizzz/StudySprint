<?php
require __DIR__ . '/vendor/autoload.php';
use App\Kernel;
use App\Entity\Etudiant;

$kernel = new Kernel($_SERVER['APP_ENV'] ?? 'dev', (bool) ($_SERVER['APP_DEBUG'] ?? 1));
$kernel->boot();

$em = $kernel->getContainer()->get('doctrine')->getManager();

try {
    $user = new Etudiant();
    $user->setEmail('test@example.com');
    // Using PHP's native password_hash. Symfony's auto hasher is compatible with standard algorithms.
    $user->setPassword(password_hash('password', PASSWORD_BCRYPT));
    $user->setRoles(['ROLE_USER']);

    $em->persist($user);
    $em->flush();
    echo "User created successfully.\n";
} catch (\Exception $e) {
    echo "Error: " . $e->getMessage() . "\n";
}
