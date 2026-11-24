package fr.loic.seances.data

import fr.loic.seances.data.Muscle.ABDOS
import fr.loic.seances.data.Muscle.BRAS
import fr.loic.seances.data.Muscle.CARDIO
import fr.loic.seances.data.Muscle.DOS
import fr.loic.seances.data.Muscle.EPAULES
import fr.loic.seances.data.Muscle.JAMBES
import fr.loic.seances.data.Muscle.PECTORAUX

/**
 * Exercices fournis avec l'app : poids du corps, haltères courts, chaise, sol, et les machines
 * les plus courantes en salle (marquées [ExerciseDef.gym]).
 * Les identifiants ne doivent jamais changer (ils sont référencés dans les sauvegardes).
 */
object Catalog {

    private fun ex(id: String, name: String, muscle: Muscle, cue: String, animId: String = id) =
        ExerciseDef(id, name, cue, muscle, animId)

    /** Exercice de salle (machine, poulie, barre, banc). */
    private fun gym(id: String, name: String, muscle: Muscle, cue: String) =
        ExerciseDef(id, name, cue, muscle, id, gym = true)

    /** Exercice de salle qui réutilise l'animation d'un autre exercice. */
    private fun gymEx(id: String, name: String, muscle: Muscle, cue: String, animId: String) =
        ExerciseDef(id, name, cue, muscle, animId, gym = true)

    val builtIn: List<ExerciseDef> = listOf(
        // Épaules
        ex("arnold_press", "Arnold Press", EPAULES,
            "Haltères devant le visage paumes vers toi, pivote et pousse au-dessus de la tête."),
        ex("lateral_raise", "Élévations latérales", EPAULES,
            "Bras presque tendus, monte les haltères jusqu'à l'horizontale, redescends lentement."),
        ex("reverse_fly", "Oiseau / Papillon inversé", EPAULES,
            "Buste penché, dos plat : ouvre les bras sur les côtés en serrant les omoplates."),
        ex("front_raise", "Élévations frontales", EPAULES,
            "Bras tendus devant toi, monte les haltères à hauteur d'épaules sans balancer le buste."),
        ex("military_press", "Développé militaire debout", EPAULES,
            "Haltères à hauteur d'oreilles, coudes sous les poignets : pousse au-dessus de la tête, gaine les abdos."),
        ex("seated_press", "Développé assis sur chaise", EPAULES,
            "Assis dos droit contre le dossier, haltères aux épaules : pousse vers le plafond."),
        ex("upright_row", "Rowing menton", EPAULES,
            "Haltères devant les cuisses : tire-les jusqu'au menton, coudes plus hauts que les mains."),

        // Pectoraux
        ex("floor_press", "Développé couché / plancher", PECTORAUX,
            "Couché, coudes au sol, pousse les haltères vers le plafond."),
        ex("dumbbell_fly", "Écartés aux haltères", PECTORAUX,
            "Couché, bras légèrement fléchis, ouvre en arc de cercle puis referme au-dessus de la poitrine."),
        ex("push_up_diamond", "Pompes diamant / inclinées", PECTORAUX,
            "Mains rapprochées sous la poitrine, corps gainé, coudes le long du corps.", "push_up"),
        ex("push_up", "Pompes classiques", PECTORAUX,
            "Mains un peu plus larges que les épaules, corps aligné de la tête aux pieds, poitrine près du sol."),
        ex("knee_push_up", "Pompes sur les genoux", PECTORAUX,
            "Genoux au sol, corps aligné des genoux à la tête : descends la poitrine entre les mains."),
        ex("decline_push_up", "Pompes pieds sur chaise", PECTORAUX,
            "Pieds sur l'assise, mains au sol : plus dur que les pompes classiques, travaille le haut des pecs."),
        ex("wide_push_up", "Pompes larges", PECTORAUX,
            "Mains bien plus écartées que les épaules : descends la poitrine, coudes vers l'extérieur.", "push_up"),
        ex("squeeze_press", "Développé serré au sol", PECTORAUX,
            "Couché, haltères collés l'un contre l'autre au-dessus de la poitrine : serre-les fort et pousse.", "floor_press"),

        // Bras
        ex("alternate_curl", "Curl alterné en supination", BRAS,
            "Coudes collés au corps, monte un haltère paume vers le haut, puis l'autre."),
        ex("skull_crusher", "Skull Crusher aux haltères", BRAS,
            "Couché, bras verticaux : plie seulement les coudes pour amener les haltères vers le front."),
        ex("hammer_curl", "Curl marteau", BRAS,
            "Prise neutre (pouces vers le haut), coudes fixes, monte les deux haltères."),
        ex("concentration_curl", "Curl concentré assis", BRAS,
            "Assis sur la chaise, coude calé contre l'intérieur de la cuisse : monte l'haltère lentement. Change de bras à mi-temps."),
        ex("overhead_triceps", "Extension triceps au-dessus de la tête", BRAS,
            "Un haltère à deux mains, coudes pointés vers le plafond : descends derrière la tête puis tends les bras."),
        ex("chair_dips", "Dips sur chaise", BRAS,
            "Mains sur le bord de l'assise, dos frôlant la chaise : plie les coudes vers l'arrière puis pousse."),
        ex("triceps_kickback", "Kickback triceps", BRAS,
            "Buste penché dos plat, coudes collés au corps et fixes : tends les bras vers l'arrière."),

        // Dos
        ex("bent_over_row", "Rowing penché lourd", DOS,
            "Buste penché dos plat, tire les haltères vers les hanches, coudes vers l'arrière."),
        ex("pullover", "Pullover aux haltères", DOS,
            "Couché, bras tendus : descends l'haltère derrière la tête puis ramène au-dessus de la poitrine."),
        ex("dead_stop_row", "Dead Stop Rows", DOS,
            "Comme le rowing, mais l'haltère repose au sol à chaque répétition."),
        ex("one_arm_row", "Rowing unilatéral appui chaise", DOS,
            "Une main sur l'assise, dos plat : tire l'haltère vers la hanche. Change de bras à mi-temps."),
        ex("superman", "Superman", DOS,
            "À plat ventre, bras tendus devant : décolle bras, poitrine et jambes, tiens une seconde, redescends."),
        ex("bird_dog", "Bird dog", DOS,
            "À quatre pattes, dos plat : tends un bras devant et la jambe opposée derrière, tiens, alterne."),

        // Jambes / fessiers
        ex("dumbbell_squat", "Squats lestés aux haltères", JAMBES,
            "Haltères le long du corps, descends cuisses parallèles au sol, dos droit."),
        ex("lunge", "Fentes lestées", JAMBES,
            "Grand pas, genou arrière vers le sol, genou avant au-dessus de la cheville."),
        ex("good_morning", "Good Morning aux haltères", JAMBES,
            "Haltères contre la poitrine, genoux souples, bascule le buste en poussant les fesses en arrière."),
        ex("jump_squat", "Squats sautés", JAMBES,
            "Descends en squat, explose vers le haut, réception souple. (Variante : squats lents avec haltères.)"),
        ex("jump_lunge", "Fentes sautées / bulgares", JAMBES,
            "En fente, saute et inverse les jambes en l'air. (Variante : fentes bulgares lestées.)"),
        ex("thruster", "Thrusters aux haltères", JAMBES,
            "Haltères aux épaules, squat puis remonte en poussant les haltères au-dessus de la tête."),
        ex("bulgarian_squat", "Squat bulgare pied sur chaise", JAMBES,
            "Pied arrière posé sur l'assise, descends le genou arrière vers le sol. Change de jambe à mi-temps."),
        ex("wall_sit", "Chaise contre le mur", JAMBES,
            "Dos plaqué au mur, cuisses parallèles au sol, genoux à 90° : tiens la position."),
        ex("glute_bridge", "Pont fessier lesté", JAMBES,
            "Couché, pieds à plat, haltère posé sur les hanches : monte le bassin en serrant les fessiers."),
        ex("romanian_deadlift", "Soulevé de terre jambes tendues", JAMBES,
            "Haltères devant les cuisses, genoux à peine fléchis : descends le long des jambes dos plat, remonte en serrant les fessiers."),
        ex("sumo_squat", "Squat sumo", JAMBES,
            "Pieds très écartés, pointes vers l'extérieur, un haltère tenu à deux mains : descends genoux ouverts."),
        ex("side_lunge", "Fentes latérales", JAMBES,
            "Pieds écartés : fléchis une jambe en poussant les fesses en arrière, l'autre reste tendue. Alterne."),
        ex("donkey_kick", "Donkey kicks", JAMBES,
            "À quatre pattes, genou plié à 90° : pousse le talon vers le plafond en serrant le fessier. Change de jambe à mi-temps."),
        ex("calf_raise", "Mollets debout", JAMBES,
            "Haltères le long du corps : monte sur la pointe des pieds, tiens une seconde, redescends lentement."),

        // Abdos / gainage
        ex("overhead_crunch", "Crunch poids au-dessus de la tête", ABDOS,
            "Bras tendus vers le plafond avec le poids, enroule le buste sans tirer sur la nuque."),
        ex("russian_twist", "Russian Twist", ABDOS,
            "Assis, buste incliné en arrière, tourne pour toucher le sol de chaque côté."),
        ex("big_l", "Le repos du Big L", ABDOS,
            "Couché, bras et jambes tendus au-dessus du sol. Lève une jambe et touche-la avec le bras opposé, alterne."),
        ex("leg_raise", "Relevés de jambes au sol", ABDOS,
            "Couché, jambes tendues : monte à la verticale, redescends sans toucher le sol."),
        ex("cross_crunch", "Crunchs obliques croisés", ABDOS,
            "Mains derrière la tête : coude vers le genou opposé, alterne les côtés."),
        ex("shoulder_taps", "Gainage dynamique", ABDOS,
            "En planche sur les mains : touche l'épaule opposée en alternant, bassin immobile."),
        ex("flutter_kicks", "Flutter kicks", ABDOS,
            "Couché, jambes tendues décollées : battements verticaux puis ciseaux horizontaux."),
        ex("hip_dips", "Planche balancés de hanches", ABDOS,
            "Planche sur les avant-bras : bascule les hanches d'un côté puis de l'autre vers le sol."),
        ex("windshield_wipers", "Windshield Wipers", ABDOS,
            "Couché, bras en croix, jambes vers le plafond : bascule-les d'un côté à l'autre."),
        ex("plank", "Gainage planche", ABDOS,
            "Sur les avant-bras, corps aligné, fessiers et abdos serrés : tiens sans creuser le dos."),
        ex("side_plank", "Gainage latéral", ABDOS,
            "Sur un avant-bras, corps de profil aligné : monte et descends le bassin. Change de côté à mi-temps."),
        ex("dead_bug", "Dead bug", ABDOS,
            "Couché, bras vers le plafond, genoux à 90° : tends un bras et la jambe opposée sans décoller le bas du dos."),

        // Cardio
        ex("mountain_climbers", "Mountain climbers", CARDIO,
            "En planche sur les mains, ramène les genoux vers la poitrine en alternant, vite."),
        ex("burpee", "Burpees", CARDIO,
            "Accroupi mains au sol, saute en planche, reviens accroupi, puis saute bras en l'air."),
        ex("jumping_jacks", "Jumping jacks", CARDIO,
            "Saute en écartant bras et jambes, puis referme. Reste léger sur les pointes."),
        ex("high_knees", "Montées de genoux", CARDIO,
            "Cours sur place en montant les genoux à hauteur de hanches, bras actifs."),
        ex("butt_kicks", "Talons-fesses", CARDIO,
            "Cours sur place en ramenant les talons vers les fesses, rapide et léger sur les pointes."),

        // Programme salle (séances du coach) : machines, poulies, kettlebell, medecine ball, landmine
        ex("snap_down", "Snap down", CARDIO,
            "Bras en l'air sur la pointe des pieds, tombe vite en demi-squat bras en arrière : réception stable et silencieuse.", "jump_squat"),
        ex("seated_jump", "Seated jump", JAMBES,
            "Assis sur un banc, pieds à plat : explose vers le haut sans élan et retombe debout, genoux souples.", "jump_squat"),
        ex("pogo_hops", "Pogo hops", CARDIO,
            "Petits sauts rapides sur la pointe des pieds, jambes presque tendues : rebondis comme un ressort.", "pogo_hop"),
        gymEx("plate_hops", "Alternating plate hops", CARDIO,
            "Un disque posé au sol : saute dessus d'un pied puis de l'autre en alternant, vite et léger.", "pogo_hop"),
        ex("lateral_step_out", "Lateral step out to balance", JAMBES,
            "Saute sur le côté sur une jambe et stabilise-toi 2 secondes, genou au-dessus du pied. Alterne.", "side_lunge"),
        ex("sl_bench_hop", "SL bench assisted hop", JAMBES,
            "Une main sur un banc, saute sur une seule jambe avec l'aide du bras : réception amortie. Change de jambe à mi-temps.", "pogo_hop"),
        gymEx("kb_swings", "KB swings", JAMBES,
            "Charnière de hanche, kettlebell entre les jambes : claque les hanches vers l'avant, la cloche monte à hauteur d'épaules.", "kb_swing"),
        gymEx("stiff_leg_box_jump", "Stiff leg low box jump", JAMBES,
            "Saute sur une box basse avec les chevilles seulement, genoux presque tendus : contact au sol très court.", "pogo_hop"),
        ex("front_squat", "Front squat", JAMBES,
            "Barre ou haltères posés devant les épaules, coudes hauts : descends buste droit, cuisses parallèles.", "dumbbell_squat"),
        gymEx("zercher_good_morning", "Zercher b-stance good morning", JAMBES,
            "Charge dans le creux des coudes, un pied en retrait sur la pointe : bascule le buste en poussant les fesses en arrière.", "good_morning"),
        gymEx("sl_knee_drive", "SL banded explosive knee drive", JAMBES,
            "Élastique au pied, sur une jambe : monte le genou vers la poitrine de façon explosive, bras opposé actif. Change de jambe à mi-temps.", "high_knees"),
        gymEx("zercher_curtsy_lunge", "Zercher landmine curtsy lunge", JAMBES,
            "Bout de la barre landmine dans les coudes : recule une jambe en croisant derrière l'autre, genou vers le sol. Alterne.", "lunge"),
        gymEx("kickback_45", "45° kick-back", JAMBES,
            "Buste incliné à 45°, appui sur une jambe : pousse l'autre talon vers l'arrière et le haut en serrant le fessier.", "donkey_kick"),
        gymEx("seated_leg_curl", "Seated leg curl", JAMBES,
            "Assis à la machine, boudin sur les chevilles : ramène les talons sous le siège, remonte lentement.", "wall_sit"),
        ex("sissy_squat", "Sissy squat", JAMBES,
            "Talons levés, tiens un support : pousse les genoux vers l'avant en basculant le buste en arrière, hanches tendues.", "wall_sit"),
        ex("tibialis_raise", "Tibialis raise", JAMBES,
            "Dos au mur, talons à 30 cm : relève les pointes de pieds le plus haut possible, redescends lentement.", "calf_raise"),
        gymEx("chin_up", "Chin-up / lat pull down", DOS,
            "Paumes vers toi, bras tendus : tire jusqu'à passer le menton au-dessus de la barre (ou barre de tirage à la poitrine).", "chin_up"),
        gymEx("landmine_row", "Landmine SA row", DOS,
            "Buste penché, une main sur le bout de la barre landmine : tire vers la hanche. Change de bras à mi-temps.", "one_arm_row"),
        gymEx("medball_chest_pass", "Lying medecine ball chest pass", PECTORAUX,
            "Couché sur le dos, ballon sur la poitrine : lance-le vers le plafond le plus fort possible, rattrape et enchaîne.", "floor_press"),
        gymEx("db_incline_press", "DB incline bench press", PECTORAUX,
            "Banc incliné à 30-45°, haltères à hauteur de poitrine : pousse vers le plafond, redescends lentement.", "floor_press"),
        gymEx("medball_slam", "Medecine ball slam", CARDIO,
            "Ballon au-dessus de la tête sur la pointe des pieds : lance-le au sol de toutes tes forces en fléchissant les hanches.", "ball_slam"),
        gymEx("rotational_slam", "Rotational slam", ABDOS,
            "Ballon au-dessus de la tête : pivote le buste et lance-le au sol à côté d'un pied. Alterne les côtés.", "ball_slam"),
        gymEx("cable_rotation", "Standing cable rotation", ABDOS,
            "De profil à la poulie, bras tendus : pivote le buste depuis les hanches, bassin gainé. Change de côté à mi-temps.", "russian_twist"),
        ex("copenhagen_plank", "Copenhagen plank", ABDOS,
            "Gainage latéral, pied du dessus posé sur un banc, l'autre jambe dans le vide : tiens. Change de côté à mi-temps.", "side_plank"),
        ex("reverse_crunch", "Reverse crunch", ABDOS,
            "Couché, genoux pliés au-dessus des hanches : enroule le bassin pour décoller les fesses, redescends lentement.", "leg_raise"),
        ex("db_rotating_curl", "DB rotating curl", BRAS,
            "Haltères en prise marteau : en montant, tourne les paumes vers le haut, coudes fixes contre le corps.", "alternate_curl"),
    ) + listOf(
        // ---------- Salle : machines, poulies, barre, banc ----------

        // Pectoraux
        gym("bench_press", "Développé couché à la barre", PECTORAUX,
            "Allongé sur le banc, pieds au sol : descends la barre jusqu'à la poitrine, coudes à 45°, puis pousse."),
        gym("chest_press", "Presse pectorale (machine)", PECTORAUX,
            "Dos collé au dossier, poignées à hauteur de poitrine : pousse devant toi sans verrouiller les coudes."),
        gym("pec_deck", "Butterfly (pec deck)", PECTORAUX,
            "Avant-bras contre les coussins : ramène les bras devant la poitrine, serre, puis ouvre lentement."),
        gym("cable_crossover", "Vis-à-vis à la poulie", PECTORAUX,
            "Poulies hautes, un pas en avant, bras légèrement fléchis : ramène les mains devant le bassin en arc de cercle."),

        // Dos
        gym("lat_pulldown", "Tirage vertical à la poulie haute", DOS,
            "Assis, cuisses calées : tire la barre jusqu'au haut de la poitrine en serrant les omoplates, remonte lentement."),
        gym("seated_cable_row", "Tirage horizontal à la poulie basse", DOS,
            "Assis, pieds sur la plateforme, dos droit : tire la poignée vers le nombril, coudes le long du corps."),
        gym("pull_up", "Tractions à la barre fixe", DOS,
            "Suspendu, mains un peu plus larges que les épaules : monte le menton au-dessus de la barre, redescends bras tendus."),
        gym("back_extension", "Banc à lombaires", DOS,
            "Hanches sur le coussin, chevilles calées : descends le buste dos plat, remonte jusqu'à l'alignement, sans cambrer."),

        // Épaules
        gym("machine_shoulder_press", "Développé épaules à la machine", EPAULES,
            "Dos contre le dossier, poignées à hauteur d'épaules : pousse vers le haut, redescends jusqu'aux oreilles."),
        gym("face_pull", "Face pull à la poulie", EPAULES,
            "Poulie à hauteur de visage, corde en main : tire vers le front en écartant les mains, coudes hauts."),

        // Bras
        gym("triceps_pushdown", "Extension triceps à la poulie haute", BRAS,
            "Coudes collés au corps et fixes : pousse la barre ou la corde vers le bas jusqu'à bras tendus."),
        gym("cable_curl", "Curl à la poulie basse", BRAS,
            "Debout face à la poulie, coudes fixes : monte la barre vers les épaules, redescends lentement."),
        gym("bar_dips", "Dips aux barres parallèles", BRAS,
            "En appui bras tendus, buste légèrement penché : descends jusqu'à 90° aux coudes, puis pousse."),

        // Jambes / fessiers
        gym("leg_press", "Presse à cuisses", JAMBES,
            "Dos plaqué, pieds largeur de hanches : descends le plateau genoux vers la poitrine, pousse sans verrouiller."),
        gym("leg_extension", "Leg extension", JAMBES,
            "Assis, rouleau sur le bas des tibias : tends les jambes, tiens une seconde, redescends lentement."),
        gym("leg_curl", "Leg curl allongé", JAMBES,
            "À plat ventre, rouleau au-dessus des talons : ramène les talons vers les fesses sans décoller le bassin."),
        gym("barbell_squat", "Squat à la barre", JAMBES,
            "Barre sur le haut du dos, pieds largeur d'épaules : descends cuisses parallèles, dos droit, remonte en poussant."),
        gym("hip_thrust", "Hip thrust", JAMBES,
            "Haut du dos contre le banc, barre sur les hanches : monte le bassin jusqu'à l'horizontale en serrant les fessiers."),

        // Abdos / gainage
        gym("captain_knee_raise", "Relevés de genoux (chaise romaine)", ABDOS,
            "Avant-bras sur les appuis, dos contre le dossier : monte les genoux vers la poitrine sans balancer."),

        // Cardio
        gym("rowing_machine", "Rameur", CARDIO,
            "Pousse d'abord avec les jambes, puis bascule le buste et tire la poignée vers le bas des côtes. Reviens dans l'ordre inverse."),
        gym("exercise_bike", "Vélo", CARDIO,
            "Selle à hauteur de hanche, jambe presque tendue en bas : pédale vite avec une résistance moyenne."),
        gym("treadmill", "Tapis de course", CARDIO,
            "Course à allure soutenue, buste droit, foulées courtes. Pense à la clé de sécurité."),
    )
}
