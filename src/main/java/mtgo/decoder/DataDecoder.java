package mtgo.decoder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author North
 */
public class DataDecoder {

    private static final String mtgoDataPath = "Data\\DOData\\";
    private static final String mtgoSetsPath = "Data\\DOSets\\";

    private String mtgoDir;
    private String[] rarity = {"C", "M", "R", "U"};
    private String[] watermark = {"???", "Plains", "Island", "Swamp", "Mountain", "Forest", "Azorius Senate", "House Dimir", "Cult of Rakdos", "Gruul", "Selesyna Conclave", "Orzhov", "Izzet", "The Golgari", "Boros Legion", "The Simic", "Mirran", "Phyrexian"};
    private List<String> sets;
    private List<String> costs;
    private List<String> flavors;
    private List<String> cardNos;
    private List<String> pt;
    private List<String> artists;
    private List<String> names;
    private List<String> abilities1;
    private List<String> abilities2;
    private List<String> abilities3;
    private List<String> abilitiesAll;
    private List<String> convertedManaCost;
    private List<String> cToken;
    private List<String> creatureType;
    private List<String> colors;

    private Map<Integer, Card> cards = new HashMap<>();
    private HashMap<Integer, Integer> unusedKeyCount = new HashMap<>();

    public DataDecoder(String mtgoDir) throws IOException {
        this.mtgoDir = mtgoDir;
        this.abilities1 = readDataFile(getDataPath("d00000001.dat"));
        this.abilities2 = readDataFile(getDataPath("d00000002.dat"));
        this.abilities3 = readDataFile(getDataPath("d00000003.dat"));
        this.abilitiesAll = readDataFile(getDataPath("d00000012.dat"));
        this.sets = readDataFile(getDataPath("d00000004.dat"));
        this.convertedManaCost = readDataFile(getDataPath("d400f5512.dat"));
        this.creatureType = readDataFile(getDataPath("d00000005.dat"));
        this.flavors = readDataFile(getDataPath("d00000006.dat"));
        this.costs = readDataFile(getDataPath("d80120409.dat"));
        this.cardNos = readDataFile(getDataPath("d00000014.dat"));
        this.pt = readDataFile(getDataPath("d00000019.dat"));
        this.artists = readDataFile(getDataPath("d8011a80a.dat"));
        this.names = readDataFile(getDataPath("d8011ae13.dat"));
        this.cToken = readDataFile(getDataPath("d4011ae14.dat"));
        this.colors = readDataFile(Paths.get("colorsNorth.dat"));
    }

    public List<Card> decode() throws IOException {
        ArrayList<String> ignoreList = new ArrayList<>();
        ignoreList.add("s.dat");
        ignoreList.add("sCG.dat");
        ignoreList.add("sGU.dat");
        ignoreList.add("sPR2.dat");
        File setsDir = new File(mtgoDir + mtgoSetsPath);
        for (File file : setsDir.listFiles()) {
            if (!ignoreList.contains(file.getName())) {
                decodeFile(file);
            }
        }

        logUnusedKeys();

        return getSortedCardList();
    }

    private List<String> readDataFile(Path file) throws IOException {
        List<String> result = Files.readAllLines(file, Charset.forName("UTF-8"));
        if (!result.isEmpty()) {
            result.set(0, result.get(0).substring(1));
        }
        return result;
    }

    private void decodeFile(File file) throws IOException {
        byte[] bytes = Files.readAllBytes(file.toPath());
        int i = 12;
        while (i < bytes.length) {
            int key = getInt(bytes, i);
            int value = getInt(bytes, i + 4);
            i += 8;
            Card card = new Card(key, value);
            do {
                key = getInt(bytes, i);
                value = getInt(bytes, i + 4);
                decodeKeyValuePair(card, key, value);
                i += 8;
            } while (key != 68);

            if (!card.isFoil() && (card.getArtist() != null || card.getCardId() == 35162)) {
                cards.put(card.getCardId(), card);
            }
        }
    }

    private int getInt(byte[] array, int offset) {
        return ((array[offset] & 255)) | ((array[offset + 1] & 255) << 8) | ((array[offset + 2] & 255) << 16) | ((array[offset + 3] & 255) << 24);
    }

    private void decodeKeyValuePair(Card card, int key, int value) {
        switch (key) {
            case 0x1:
                card.setAbility(abilities1.get(value));
                break;
            case 0x2:
                card.setAbility(abilities2.get(value));
                break;
            case 0x3:
                card.setAbility(abilities3.get(value));
                break;
            case 0x4:
                card.setSet(sets.get(value));
                break;
            case 0x5:
                card.setCreatureType(creatureType.get(value));
                break;
            case 0x6:
                card.setFlavor(flavors.get(value));
                break;
            case 0x11:
                card.setRarity(rarity[value]);
                break;
            case 0x12:
                card.setAbility(abilitiesAll.get(value));
                break;
            case 0x13:
                card.setBaseCardId(value);
                break;
            case 0x14:
                card.setCardNo(cardNos.get(value));
                break;
            case 0x19:
                card.setPt(pt.get(value));
                break;
            case 0x4012461b:
                card.setImage(Integer.toString(value));
                break;
            case 0x8011ae13:
                card.setName(names.get(value));
                break;
            case 0x400f5512:
                card.setConvertedManaCost(convertedManaCost.get(value));
                break;
            case 0x8011a80a:
                card.setArtist(artists.get(value));
                break;
            case 0x80120409:
                card.setCost(costs.get(value));
                break;
            case 0x4011ae14:
                card.setcToken(cToken.get(value));
                break;
            case 0x4012001b:
                card.addType("Land");
                break;
            case 0x40120010:
                card.addType("Creature");
                break;
            case 0x4012000d:
                card.addType("Artifact");
                break;
            case 0x40120004:
                card.addType("Instant");
                break;
            case 0x40122d07:
                card.addType("Sorcery");
                break;
            case 0x4012611f:
                card.addType("Planeswalker");
                break;
            case 0x40120011:
                card.addType("Enchantment");
                break;
            case 0x40120015:
                card.addType("Equipment");
                break;
            case 0x4012001d:
                card.addType("Aura");
                break;
            case 0x40126123:
                card.addType("Tribal");
                break;
            case 0x400f550f:
                card.setColor(colors.get(value));
                break;
            case 0x4012cb05:
                card.setWatermark(watermark[value]);
                break;
            case 0x400f9f45:
                card.setFoil(true);
                break;
            default:
                card.addUnusedPair(key, value);
                Integer count = unusedKeyCount.get(key);
                count = count == null ? 1 : count + 1;
                unusedKeyCount.put(key, count);
                if (unusedKeyCount.get(key) != null) {
                    unusedKeyCount.put(key, unusedKeyCount.get(key) + 1);
                } else {
                    unusedKeyCount.put(key, 1);
                }
        }
    }

    private void logUnusedKeys() {
        for (Integer key : unusedKeyCount.keySet()) {
            if (unusedKeyCount.get(key) > 500) {
                System.out.println(Integer.toHexString(key) + " -> " + unusedKeyCount.get(key));
            }
        }
    }

    private List<Card> getSortedCardList() {
        List<Card> cardList = new ArrayList<>();
        int x = 0;
        for (Integer cardId : cards.keySet()) {
            Card card = cards.get(cardId);
            if ("Phyrexian Juggernaut".equals(card.getName())) {
                card.printUnused();
            }
            findParentIdKey(card);
            if (card.getBaseCardId() != null) {
                Card baseCard = cards.get(card.getBaseCardId());
                if (baseCard != null) {
                    card.copyFromBaseCard(baseCard);
                }
            }
            cardList.add(card);
        }
        Collections.sort(cardList, new CardComparator());
        return cardList;
    }

    private Path getDataPath(String fileName) {
        return Paths.get(mtgoDir + mtgoDataPath + fileName);
    }

    private void findParentIdKey(Card card) {
        if ("Teetering Peaks".equals(card.getName())) {
            System.out.println("ID: " + card.getCardId());
        }
        if ("ZEN".equals(card.getSet()) && "226/249".equals(card.getCardNo())) {
            System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>");
            card.printUnused();
            System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>");
        }
    }
}
