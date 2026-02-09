<?php

namespace App\Controller;

use App\Entity\Objectif;
use App\Entity\Tache;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Annotation\Route;

class HomeController extends AbstractController
{
    #[Route('/', name: 'app_home')]
    public function index(EntityManagerInterface $entityManager): Response
    {
        if (!$this->getUser()) {
            return $this->redirectToRoute('app_login');
        }

        $user = $this->getUser();
        $objectiveRepo = $entityManager->getRepository(Objectif::class);
        $taskRepo = $entityManager->getRepository(Tache::class);

        // 1. Priority of the day: Find urgent objective (closest deadline)
        $priorityObjective = $objectiveRepo->findOneBy(
            ['etudiant' => $user, 'statut' => 'EN_COURS'],
            ['dateFin' => 'ASC']
        );

        // 2. Stats
        // Total completed sessions (using tasks as proxy for now)
        // We'll count completed tasks in the last 7 days for "Sessions cette semaine"
        $weekStart = new \DateTime('-7 days');
        // This is a rough approximation since we don't have a "completedAt" field, 
        // we'll just count all completed tasks for the demo stats
        $completedTasks = $taskRepo->findBy(['statut' => 'TERMINE']);
        // Filter by user via objective (inefficient in loop but ok for prototype)
        $userCompletedTasks = array_filter($completedTasks, function ($t) use ($user) {
            return $t->getObjectif()->getEtudiant() === $user;
        });

        $sessionsCount = count($userCompletedTasks);

        // 3. To Do list (Tasks for today/pending)
        // Get all pending tasks for user
        $allObjectives = $objectiveRepo->findBy(['etudiant' => $user]);
        $todoTasks = [];
        foreach ($allObjectives as $obj) {
            foreach ($obj->getTaches() as $t) {
                if ($t->getStatut() !== 'TERMINE') {
                    $todoTasks[] = $t;
                }
            }
        }
        // Slice to show only first 5
        $todoTasks = array_slice($todoTasks, 0, 5);

        return $this->render('dashboard/index.html.twig', [
            'priorityObjective' => $priorityObjective,
            'sessionsCount' => $sessionsCount,
            'todoTasks' => $todoTasks,
            // Hardcoded placeholders for missing entities
            'totalTime' => '8h45',
            'quizScore' => '24/28',
            'weeklyGoal' => 75
        ]);
    }
}
