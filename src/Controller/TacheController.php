<?php

namespace App\Controller;

use App\Entity\Objectif;
use App\Entity\Tache;
use App\Form\TacheType;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Annotation\Route;

#[Route('/tache')]
class TacheController extends AbstractController
{
    #[Route('/', name: 'app_tache_index', methods: ['GET'])]
    public function index(EntityManagerInterface $entityManager): Response
    {
        $this->denyAccessUnlessGranted('IS_AUTHENTICATED_FULLY');

        // Find tasks for objectives owned by current user
        $taches = $entityManager->getRepository(Tache::class)->createQueryBuilder('t')
            ->join('t.objectif', 'o')
            ->where('o.etudiant = :user')
            ->setParameter('user', $this->getUser())
            ->getQuery()
            ->getResult();

        return $this->render('tache/index.html.twig', [
            'taches' => $taches,
        ]);
    }

    #[Route('/new', name: 'app_tache_new_standalone', methods: ['GET', 'POST'])]
    public function newStandalone(Request $request, EntityManagerInterface $entityManager): Response
    {
        $this->denyAccessUnlessGranted('IS_AUTHENTICATED_FULLY');

        $tache = new Tache();
        $form = $this->createForm(TacheType::class, $tache, ['user' => $this->getUser()]);
        $form->handleRequest($request);

        if ($form->isSubmitted() && $form->isValid()) {
            $entityManager->persist($tache);
            $entityManager->flush();

            return $this->redirectToRoute('app_tache_index', [], Response::HTTP_SEE_OTHER);
        }

        return $this->render('tache/new.html.twig', [
            'tache' => $tache,
            'form' => $form->createView(),
        ]);
    }

    #[Route('/new/{id}', name: 'app_tache_new', methods: ['GET', 'POST'])]
    public function new(int $id, Request $request, EntityManagerInterface $entityManager): Response
    {
        $objectif = $entityManager->getRepository(Objectif::class)->find($id);

        if (!$objectif) {
            throw $this->createNotFoundException('L\'objectif n\'existe pas.');
        }

        // Check ownership of objective
        if ($objectif->getEtudiant() !== $this->getUser()) {
            throw $this->createAccessDeniedException();
        }

        $tache = new Tache();
        $tache->setObjectif($objectif);

        $form = $this->createForm(TacheType::class, $tache, ['user' => $this->getUser()]);
        $form->handleRequest($request);

        if ($form->isSubmitted() && $form->isValid()) {
            $entityManager->persist($tache);
            $entityManager->flush();

            return $this->redirectToRoute('app_objectif_show', ['id' => $objectif->getId()], Response::HTTP_SEE_OTHER);
        }

        return $this->render('tache/new.html.twig', [
            'tache' => $tache,
            'form' => $form->createView(),
            'objectif' => $objectif,
        ]);
    }

    #[Route('/{id}/show', name: 'app_tache_show', methods: ['GET'])]
    public function show(int $id, EntityManagerInterface $entityManager): Response
    {
        $tache = $entityManager->getRepository(Tache::class)->find($id);

        if (!$tache) {
            throw $this->createNotFoundException('La tâche n\'existe pas.');
        }

        if ($tache->getObjectif()->getEtudiant() !== $this->getUser()) {
            throw $this->createAccessDeniedException();
        }

        return $this->render('tache/show.html.twig', [
            'tache' => $tache,
        ]);
    }

    #[Route('/{id}/edit', name: 'app_tache_edit', methods: ['GET', 'POST'])]
    public function edit(int $id, Request $request, EntityManagerInterface $entityManager): Response
    {
        $tache = $entityManager->getRepository(Tache::class)->find($id);

        if (!$tache) {
            throw $this->createNotFoundException('La tâche n\'existe pas.');
        }

        // Check ownership
        if ($tache->getObjectif()->getEtudiant() !== $this->getUser()) {
            throw $this->createAccessDeniedException();
        }

        $form = $this->createForm(TacheType::class, $tache, ['user' => $this->getUser()]);
        $form->handleRequest($request);

        if ($form->isSubmitted() && $form->isValid()) {
            $entityManager->flush();

            return $this->redirectToRoute('app_objectif_show', ['id' => $tache->getObjectif()->getId()], Response::HTTP_SEE_OTHER);
        }

        return $this->render('tache/edit.html.twig', [
            'tache' => $tache,
            'form' => $form->createView(),
        ]);
    }

    #[Route('/{id}', name: 'app_tache_delete', methods: ['POST'])]
    public function delete(int $id, Request $request, EntityManagerInterface $entityManager): Response
    {
        $tache = $entityManager->getRepository(Tache::class)->find($id);

        if (!$tache) {
            throw $this->createNotFoundException('La tâche n\'existe pas.');
        }

        // Check ownership
        if ($tache->getObjectif()->getEtudiant() !== $this->getUser()) {
            throw $this->createAccessDeniedException();
        }

        $objectifId = $tache->getObjectif()->getId();

        if ($this->isCsrfTokenValid('delete' . $tache->getId(), $request->request->get('_token'))) {
            $entityManager->remove($tache);
            $entityManager->flush();
        }

        return $this->redirectToRoute('app_objectif_show', ['id' => $objectifId], Response::HTTP_SEE_OTHER);
    }

    #[Route('/{id}/toggle', name: 'app_tache_toggle', methods: ['POST'])]
    public function toggle(int $id, EntityManagerInterface $entityManager): Response
    {
        $tache = $entityManager->getRepository(Tache::class)->find($id);

        if (!$tache) {
            throw $this->createNotFoundException('La tâche n\'existe pas.');
        }

        if ($tache->getObjectif()->getEtudiant() !== $this->getUser()) {
            throw $this->createAccessDeniedException();
        }

        if ($tache->getStatut() === 'TERMINE') {
            $tache->setStatut('EN_COURS');
            // If we reopen a task, the objective cannot be TERMINE anymore
            if ($tache->getObjectif()->getStatut() === 'TERMINE') {
                $tache->getObjectif()->setStatut('EN_COURS');
            }
        } else {
            $tache->setStatut('TERMINE');

            // Check if all tasks are done
            $allDone = true;
            foreach ($tache->getObjectif()->getTaches() as $t) {
                if ($t->getId() === $tache->getId()) {
                    continue;
                }
                if ($t->getStatut() !== 'TERMINE') {
                    $allDone = false;
                    break;
                }
            }

            if ($allDone) {
                $tache->getObjectif()->setStatut('TERMINE');
            }
        }

        $entityManager->flush();

        return $this->redirectToRoute('app_objectif_show', ['id' => $tache->getObjectif()->getId()], Response::HTTP_SEE_OTHER);
    }
}
