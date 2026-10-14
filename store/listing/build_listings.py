#!/usr/bin/env python3
"""Writes store/listing/<locale>.txt for the Play Console and checks the length limits
(title 30, short description 80, full description 4000). Run: python3 store/listing/build_listings.py"""
import os, sys
HERE = os.path.dirname(os.path.abspath(__file__))

LANGS = ['English, Español, Deutsch, Français, Italiano, Português, हिन्दी, Bahasa Indonesia'] * 8

L = {}

L['en-GB'] = dict(
 title="Photo Collage | Maker & Editor",
 short="Make photo collages with 100+ layouts, text, stickers and filters. No account.",
 full="""Make photo collages that look great, in seconds. Pick your photos, choose from more than 100 layouts and make it yours.

CREATE
• Up to 12 photos in one collage
• More than 100 layouts: grids, hearts, clover, honeycomb, letters, words and more
• Design your own layout: draw cells, pick shapes and save it to My layouts
• Ready-made sizes for Instagram, Facebook, Pinterest, TikTok, YouTube, WhatsApp and print

MAKE IT YOURS
• Zoom, move, rotate and flip each photo
• Adjust brightness, contrast and saturation, or pick a filter
• Colours, gradients, textures or your own photo as the background
• Frames and borders: polaroid, film strip, torn paper and more
• Rounded corners, spacing and shapes for every cell

TEXT AND STICKERS
• More than 50 fonts, colours, gradients, outlines, shadows and curved text
• Ready-made text styles to start from
• Emoji and shape stickers

SAVE AND SHARE
• Save to your gallery in high quality, or share straight to your favourite apps
• A History of every collage you have saved
• Available in English, Español, Deutsch, Français, Italiano, Português, हिन्दी and Bahasa Indonesia

PRIVATE
Your photos stay on your phone. There is no account and no sign-up.

FREE OR PRO
Photo Collage is free to use. Saved collages carry a small Photo Collage mark in the corner. Photo Collage Pro is a monthly subscription, billed through Google Play and cancelled at any time there, and it removes the mark.""")

L['es-ES'] = dict(
 title="Photo Collage: Crear y Editar",
 short="Crea collages con más de 100 diseños, texto, pegatinas y filtros. Sin cuenta.",
 full="""Crea collages de fotos que lucen genial, en segundos. Elige tus fotos, escoge entre más de 100 diseños y hazlo tuyo.

CREA
• Hasta 12 fotos en un mismo collage
• Más de 100 diseños: cuadrículas, corazones, trébol, panal, letras, palabras y más
• Diseña tu propio diseño: dibuja celdas, elige formas y guárdalo en Mis diseños
• Tamaños listos para Instagram, Facebook, Pinterest, TikTok, YouTube, WhatsApp e impresión

HAZLO TUYO
• Amplía, mueve, gira y voltea cada foto
• Ajusta brillo, contraste y saturación, o elige un filtro
• Colores, degradados, texturas o tu propia foto como fondo
• Marcos y bordes: polaroid, tira de película, papel rasgado y más
• Esquinas redondeadas, espaciado y formas para cada celda

TEXTO Y PEGATINAS
• Más de 50 fuentes, colores, degradados, contornos, sombras y texto curvo
• Estilos de texto listos para empezar
• Pegatinas de emoji y de formas

GUARDA Y COMPARTE
• Guarda en tu galería en alta calidad o comparte directamente en tus apps favoritas
• Un Historial de todos los collages que has guardado
• Disponible en English, Español, Deutsch, Français, Italiano, Português, हिन्दी y Bahasa Indonesia

PRIVADO
Tus fotos se quedan en tu teléfono. No hay cuenta ni registro.

GRATIS O PRO
Photo Collage es gratis. Los collages guardados llevan una pequeña marca de Photo Collage en la esquina. Photo Collage Pro es una suscripción mensual, que se cobra a través de Google Play y se cancela en cualquier momento allí, y quita la marca.""")

L['de-DE'] = dict(
 title="Photo Collage: Maker & Editor",
 short="Fotocollagen mit über 100 Layouts, Text, Stickern und Filtern. Ohne Konto.",
 full="""Erstelle Fotocollagen, die toll aussehen, in Sekunden. Wähle deine Fotos, such dir eines von über 100 Layouts aus und mach es zu deinem.

ERSTELLEN
• Bis zu 12 Fotos in einer Collage
• Über 100 Layouts: Raster, Herzen, Kleeblatt, Waben, Buchstaben, Wörter und mehr
• Entwirf dein eigenes Layout: Zellen zeichnen, Formen wählen und unter Meine Layouts speichern
• Fertige Größen für Instagram, Facebook, Pinterest, TikTok, YouTube, WhatsApp und Druck

MACH ES DIR ZU EIGEN
• Jedes Foto zoomen, verschieben, drehen und spiegeln
• Helligkeit, Kontrast und Sättigung anpassen oder einen Filter wählen
• Farben, Verläufe, Texturen oder dein eigenes Foto als Hintergrund
• Rahmen und Ränder: Polaroid, Filmstreifen, gerissenes Papier und mehr
• Abgerundete Ecken, Abstände und Formen für jede Zelle

TEXT UND STICKER
• Über 50 Schriften, Farben, Verläufe, Konturen, Schatten und gebogener Text
• Fertige Textstile als Ausgangspunkt
• Emoji- und Form-Sticker

SPEICHERN UND TEILEN
• In hoher Qualität in der Galerie speichern oder direkt in deinen Lieblings-Apps teilen
• Ein Verlauf aller gespeicherten Collagen
• Verfügbar auf English, Español, Deutsch, Français, Italiano, Português, हिन्दी und Bahasa Indonesia

PRIVAT
Deine Fotos bleiben auf deinem Handy. Es gibt kein Konto und keine Anmeldung.

KOSTENLOS ODER PRO
Photo Collage ist kostenlos. Gespeicherte Collagen tragen ein kleines Photo-Collage-Zeichen in der Ecke. Photo Collage Pro ist ein monatliches Abo, das über Google Play abgerechnet und dort jederzeit gekündigt wird, und es entfernt das Zeichen.""")

L['fr-FR'] = dict(
 title="Photo Collage: Maker & Editor",
 short="Collages photo : 100+ modèles, texte, stickers et filtres. Sans compte.",
 full="""Créez des collages photo qui ont fière allure, en quelques secondes. Choisissez vos photos, parmi plus de 100 modèles, et faites-en le vôtre.

CRÉER
• Jusqu’à 12 photos dans un même collage
• Plus de 100 modèles : grilles, cœurs, trèfle, nid d’abeille, lettres, mots et plus encore
• Créez votre propre modèle : dessinez des cellules, choisissez des formes et enregistrez-le dans Mes modèles
• Tailles prêtes pour Instagram, Facebook, Pinterest, TikTok, YouTube, WhatsApp et l’impression

PERSONNALISER
• Zoomez, déplacez, faites pivoter et retournez chaque photo
• Réglez la luminosité, le contraste et la saturation, ou choisissez un filtre
• Couleurs, dégradés, textures ou votre propre photo en arrière-plan
• Cadres et bordures : polaroid, pellicule, papier déchiré et plus
• Coins arrondis, espacement et formes pour chaque cellule

TEXTE ET STICKERS
• Plus de 50 polices, couleurs, dégradés, contours, ombres et texte courbé
• Styles de texte prêts à l’emploi
• Stickers emoji et formes

ENREGISTRER ET PARTAGER
• Enregistrez dans votre galerie en haute qualité ou partagez directement vers vos applis préférées
• Un Historique de tous vos collages enregistrés
• Disponible en English, Español, Deutsch, Français, Italiano, Português, हिन्दी et Bahasa Indonesia

PRIVÉ
Vos photos restent sur votre téléphone. Pas de compte, pas d’inscription.

GRATUIT OU PRO
Photo Collage est gratuit. Les collages enregistrés portent un petit repère Photo Collage dans le coin. Photo Collage Pro est un abonnement mensuel, facturé via Google Play et résiliable à tout moment depuis Google Play, qui supprime ce repère.""")

L['it-IT'] = dict(
 title="Photo Collage: Maker & Editor",
 short="Crea collage con oltre 100 layout, testo, sticker e filtri. Senza account.",
 full="""Crea collage di foto che hanno un gran aspetto, in pochi secondi. Scegli le tue foto, scegli tra oltre 100 layout e rendilo tuo.

CREA
• Fino a 12 foto in un solo collage
• Oltre 100 layout: griglie, cuori, quadrifoglio, nido d’ape, lettere, parole e altro
• Progetta il tuo layout: disegna le celle, scegli le forme e salvalo in I miei layout
• Dimensioni pronte per Instagram, Facebook, Pinterest, TikTok, YouTube, WhatsApp e stampa

PERSONALIZZA
• Ingrandisci, sposta, ruota e capovolgi ogni foto
• Regola luminosità, contrasto e saturazione, oppure scegli un filtro
• Colori, sfumature, texture o una tua foto come sfondo
• Cornici e bordi: polaroid, pellicola, carta strappata e altro
• Angoli arrotondati, spaziatura e forme per ogni cella

TESTO E STICKER
• Oltre 50 caratteri, colori, sfumature, contorni, ombre e testo curvo
• Stili di testo pronti da cui partire
• Sticker emoji e di forme

SALVA E CONDIVIDI
• Salva nella galleria in alta qualità o condividi direttamente nelle tue app preferite
• Una Cronologia di tutti i collage salvati
• Disponibile in English, Español, Deutsch, Français, Italiano, Português, हिन्दी e Bahasa Indonesia

PRIVATO
Le tue foto restano sul telefono. Nessun account, nessuna registrazione.

GRATIS O PRO
Photo Collage è gratuito. I collage salvati hanno un piccolo segno Photo Collage nell’angolo. Photo Collage Pro è un abbonamento mensile, addebitato tramite Google Play e annullabile in qualsiasi momento da lì, che rimuove il segno.""")

L['pt-BR'] = dict(
 title="Photo Collage: Maker & Editor",
 short="Crie colagens com mais de 100 layouts, texto, adesivos e filtros. Sem conta.",
 full="""Crie colagens de fotos que ficam ótimas, em segundos. Escolha suas fotos, selecione entre mais de 100 layouts e deixe do seu jeito.

CRIE
• Até 12 fotos em uma única colagem
• Mais de 100 layouts: grades, corações, trevo, favo de mel, letras, palavras e mais
• Crie seu próprio layout: desenhe células, escolha formas e salve em Meus layouts
• Tamanhos prontos para Instagram, Facebook, Pinterest, TikTok, YouTube, WhatsApp e impressão

DEIXE DO SEU JEITO
• Amplie, mova, gire e espelhe cada foto
• Ajuste brilho, contraste e saturação, ou escolha um filtro
• Cores, gradientes, texturas ou sua própria foto como fundo
• Molduras e bordas: polaroid, filme, papel rasgado e mais
• Cantos arredondados, espaçamento e formas para cada célula

TEXTO E ADESIVOS
• Mais de 50 fontes, cores, gradientes, contornos, sombras e texto curvo
• Estilos de texto prontos para começar
• Adesivos de emoji e de formas

SALVE E COMPARTILHE
• Salve na galeria em alta qualidade ou compartilhe direto nos seus apps favoritos
• Um Histórico de todas as colagens salvas
• Disponível em English, Español, Deutsch, Français, Italiano, Português, हिन्दी e Bahasa Indonesia

PRIVADO
Suas fotos ficam no seu celular. Não há conta nem cadastro.

GRÁTIS OU PRO
O Photo Collage é grátis. As colagens salvas levam uma pequena marca do Photo Collage no canto. O Photo Collage Pro é uma assinatura mensal, cobrada pelo Google Play e cancelável a qualquer momento por lá, que remove a marca.""")

L['hi-IN'] = dict(
 title="Photo Collage | मेकर और एडिटर",
 short="100+ लेआउट, टेक्स्ट, स्टिकर और फ़िल्टर के साथ फ़ोटो कोलाज बनाएँ। बिना अकाउंट।",
 full="""कुछ ही सेकंड में शानदार फ़ोटो कोलाज बनाएँ। अपनी फ़ोटो चुनें, 100 से ज़्यादा लेआउट में से पसंद का लेआउट चुनें और उसे अपना बनाएँ।

बनाएँ
• एक कोलाज में 12 तक फ़ोटो
• 100 से ज़्यादा लेआउट: ग्रिड, दिल, क्लोवर, हनीकॉम्ब, अक्षर, शब्द और भी बहुत कुछ
• अपना लेआउट खुद डिज़ाइन करें: सेल बनाएँ, आकार चुनें और उसे "मेरे लेआउट" में सेव करें
• Instagram, Facebook, Pinterest, TikTok, YouTube, WhatsApp और प्रिंट के लिए तैयार आकार

अपना बनाएँ
• हर फ़ोटो को ज़ूम करें, खिसकाएँ, घुमाएँ और पलटें
• चमक, कंट्रास्ट और संतृप्ति बदलें, या कोई फ़िल्टर चुनें
• पृष्ठभूमि के लिए रंग, ग्रेडिएंट, बनावट या अपनी ही फ़ोटो
• फ़्रेम और बॉर्डर: पोलेरॉइड, फ़िल्म स्ट्रिप, फटा हुआ कागज़ और भी बहुत कुछ
• हर सेल के लिए गोल कोने, दूरी और आकार

टेक्स्ट और स्टिकर
• 50 से ज़्यादा फ़ॉन्ट, रंग, ग्रेडिएंट, आउटलाइन, छाया और घुमावदार टेक्स्ट
• शुरू करने के लिए तैयार टेक्स्ट शैलियाँ
• इमोजी और आकार वाले स्टिकर

सेव और शेयर करें
• उच्च गुणवत्ता में गैलरी में सेव करें, या सीधे अपने पसंदीदा ऐप में शेयर करें
• सेव किए गए सभी कोलाज का इतिहास
• English, Español, Deutsch, Français, Italiano, Português, हिन्दी और Bahasa Indonesia में उपलब्ध

निजी
आपकी फ़ोटो आपके फ़ोन में ही रहती हैं। कोई अकाउंट नहीं, कोई साइन-अप नहीं।

मुफ़्त या Pro
Photo Collage मुफ़्त है। सेव किए गए कोलाज के कोने में एक छोटा Photo Collage चिह्न रहता है। Photo Collage Pro एक मासिक सदस्यता है, जिसका भुगतान Google Play से होता है और जिसे वहीं कभी भी रद्द किया जा सकता है। यह चिह्न हटा देती है।""")

L['id'] = dict(
 title="Photo Collage: Maker & Editor",
 short="Buat kolase foto dengan 100+ tata letak, teks, stiker, dan filter. Tanpa akun.",
 full="""Buat kolase foto yang tampil keren dalam hitungan detik. Pilih foto Anda, pilih dari lebih dari 100 tata letak, lalu jadikan milik Anda.

BUAT
• Hingga 12 foto dalam satu kolase
• Lebih dari 100 tata letak: kisi, hati, semanggi, sarang lebah, huruf, kata, dan lainnya
• Rancang tata letak Anda sendiri: gambar sel, pilih bentuk, dan simpan di Tata letak saya
• Ukuran siap pakai untuk Instagram, Facebook, Pinterest, TikTok, YouTube, WhatsApp, dan cetak

BUAT JADI MILIK ANDA
• Perbesar, geser, putar, dan balik setiap foto
• Atur kecerahan, kontras, dan saturasi, atau pilih filter
• Warna, gradasi, tekstur, atau foto Anda sendiri sebagai latar
• Bingkai dan tepi: polaroid, strip film, kertas sobek, dan lainnya
• Sudut membulat, jarak, dan bentuk untuk setiap sel

TEKS DAN STIKER
• Lebih dari 50 font, warna, gradasi, garis tepi, bayangan, dan teks melengkung
• Gaya teks siap pakai sebagai awal
• Stiker emoji dan bentuk

SIMPAN DAN BAGIKAN
• Simpan ke galeri dalam kualitas tinggi, atau bagikan langsung ke aplikasi favorit Anda
• Riwayat semua kolase yang telah Anda simpan
• Tersedia dalam English, Español, Deutsch, Français, Italiano, Português, हिन्दी, dan Bahasa Indonesia

PRIVAT
Foto Anda tetap di ponsel Anda. Tidak ada akun dan tidak perlu mendaftar.

GRATIS ATAU PRO
Photo Collage gratis digunakan. Kolase yang disimpan memiliki tanda kecil Photo Collage di sudutnya. Photo Collage Pro adalah langganan bulanan, ditagih melalui Google Play dan bisa dibatalkan kapan saja di sana, yang menghapus tanda tersebut.""")

ok = True
for loc, d in L.items():
    t, s, f = len(d['title']), len(d['short']), len(d['full'])
    flag = '' if (t <= 30 and s <= 80 and f <= 4000) else '  <-- OVER LIMIT'
    if flag: ok = False
    print(f'{loc:6} title {t:2}/30  short {s:2}/80  full {f:4}/4000{flag}')
    with open(os.path.join(HERE, loc + '.txt'), 'w', encoding='utf-8') as out:
        out.write(f"TITLE ({t}/30)\n{d['title']}\n\nSHORT DESCRIPTION ({s}/80)\n{d['short']}\n\nFULL DESCRIPTION ({f}/4000)\n{d['full']}\n")
sys.exit(0 if ok else 1)
