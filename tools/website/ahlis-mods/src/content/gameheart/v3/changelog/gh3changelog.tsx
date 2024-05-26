import { Change, Changelog, Post } from '../../../../types/Post';
import gh3_34 from '../../../../assets/gameheart/v3/gh3_34.avif';
import gh3_35 from '../../../../assets/gameheart/v3/gh3_35.avif';
import gh3_36 from '../../../../assets/gameheart/v3/gh3_36.avif';
import gh3_37 from '../../../../assets/gameheart/v3/gh3_37.avif';

export const gh3changelog: Post[] = [
  {
    id: '37',
    title: '3.7',
    image: [gh3_37, gh3_36],
    createdAt: '23rd May 2024',
    download:
      'https://www.dropbox.com/scl/fi/v0k6pbt5xkd9rcotsjoba/GameHeart_3.7.SC2Interface?rlkey=hb7k60nwlyu5m66zrg95ih42m&dl=1',
    version: '3.7',
    date: '23rd May 2024',
    changes: [
      'fixed workers killed notification background not sliding in',
      'added worker count to 3vs3, 4vs4 and FFA player cards',
      'fixed 3vs3 shwoing 8 slots',
      'fixed 3vs3 and 4vs4 not using team slots (upper vs lower row in UI)',
      'added selected player information from FFA to 3vs3 and 4vs4',
    ],
  } as Changelog,
  {
    id: '36',
    title: '3.6',
    image: gh3_36,
    createdAt: '5th January 2023',
    download: 'https://www.dropbox.com/s/1edtu9ci9z767cg/GameHeart_3.6.SC2Interface?dl=1',
    version: '3.6',
    date: '5th January 2023',
    changes: [
      'added customizable Battle Report shortcut',
      'added Battle Report to the dialog auto-hiding system that makes dialogs hide each other when opened',
      "renamed to 'GameHeart 3' in the game options' dropdowns",
      'added support for AhliObs extension mods (Battle Report, Production Tab)',
      'fixed cargo displaying a solid square image instead of a colored border',
      'fixed Workers killed Notifications not being visible in some extension mods',
    ],
  } as Changelog,
  {
    id: '35',
    title: '3.5',
    image: gh3_35,
    createdAt: '26th June 2022',
    download: 'https://www.dropbox.com/s/egixonbgilk3no8/GameHeart_3.5.SC2Interface?dl=1',
    version: '3.5',
    date: '26th June 2022',
    changes: [
      new Change('added support for HomeStoryCup XX extension mod:', [
        'Resources Gathered Graph on NumPad4',
        'Resources Lost Difference Graph on NumPad3',
        'Production Tab of the Leaderpanel will show a Chrono Boost indicator',
      ]),
      'stylized graphs',
      'fixed workers killed notification slots being visible despite the feature being disabled in an extension mod',
      new Change('production tab:', [
        'progress bars now have a background indicating how much progress is left',
        'is now able to display alliance colors instead of white during player vision',
      ]),
      new Change('info panel:', [
        'a single selected unit now tints armor, weapons and progress in player color',
        'unit owner colors can now display alliance colors instead of white during player vision',
        'buttons are now muted',
      ]),
      'selecting different leaderpanel tabs does not play a sound anymore',
    ],
  } as Changelog,
  {
    id: '34',
    title: '3.4',
    image: gh3_34,
    createdAt: '19th March 2021',
    download: 'https://www.dropbox.com/s/wbug98qxapkwoo1/GameHeart_3.4.SC2Interface?dl=1',
    version: '3.4',
    date: '19th March 2021',
    changes: [
      new Change('fixed issues with selecting multiple units and colored borders:', [
        'removed update delay of colored unit frame border when selecting additional units',
        'fixed button clicks on unit frame borders being ignored',
      ]),
      "added fallback text to player intro panel's score and rank titles when not defined in Logos.SC2Mod",
    ],
  } as Changelog,
  {
    id: '33',
    title: '3.3',
    createdAt: '22nd December 2019',
    download: 'https://www.dropbox.com/s/5d6j93g3e48ganf/GameHeart_3.3.SC2Interface?dl=1',
    version: '3.3',
    date: '22nd December 2019',
    changes: [
      "fixed control groups' appearance",
      "fixed social button's appearance",
      'fixed friend list and party invites spawning off-screen the first time the social panel is opened',
      'fixed tooltip backgrounds',
      "fixed race icon shortcut's starting state",
      'fixed the second slot of player intro panels',
      'fixed the offset of the player name when the race icon is hidden in FFA',
      'fixed tournament match panels spawning off-screen',
      'added 4 more score values to the raw scores panel: MineralsLostUnits, VespeneLostUnits, ResourcesGathered, UnitsProduced',
      new Change('reworked units lost panel:', [
        'structures added into the same view; structures lost panel removed',
        'resources lost added; the position is dynamic to save space when the panel is empty',
        'size of icons and labels reduced to be about as big as the production tab',
      ]),
      'comparison bars of the leaderpanel are now hidden if the value is 0',
      'in FFA mode the supply and race icon is now hidden when no unit is selected',
      "added toggle UI hotkey into message log's window (ctrl + alt + U)",
      new Change('selecting multiple units will show a player-colored border', [
        new Change('known problems:', [
          'selecting an additional unit has an update delay of one second',
          'mouse clicks on a unit button are ignored once per second',
        ]),
      ]),
      new Change('workers killed notification animates when a worker is killed', [
        'requires an extension mod update (e.g. it is implemented in the "HomeStoryCup XX" extension mod)',
      ]),
    ],
  } as Changelog,
  {
    id: '32',
    title: '3.2',
    createdAt: '27th July 2018',
    download: 'https://www.dropbox.com/s/r9zst19ecuo422q/GameHeart_3.2.SC2Interface?dl=1',
    version: '3.2',
    date: '27th July 2018',
    changes: [
      "fixed Control Groups' position and tooltip colors",
      new Change('3vs3, 4vs4 and FFA will now create a new panel in the center bottom', [
        "FFA's support is currently limited: it will currently show 8 slots, not more and not less",
      ]),
      'player names in bottom bar now use a smaller font instead of being cut',
      'fixed a little gap that can appear between the minimap border image and the minimap background',
    ],
    text: 'Unfortunately, I somehow failed to post 3.1 when I created that.',
  } as Changelog,
  {
    id: '30',
    title: '3.0',
    createdAt: '14th November 2017',
    download: 'https://www.dropbox.com/s/h1ae4mck9lumfc7/GameHeart_3.0.SC2Interface?dl=1',
    version: '3.0',
    date: '14th November 2017',
    changes: [
      new Change('added support for 1vs1 (includes Archon mode) and 2vs2', [
        'other modes show the default resource panel in the top right for the selected player',
      ]),
      "cleaned up minimap's border; it is now 2 pixels wider, bottom bar is 2 pixels shorter",
      new Change('Control+Shift+R will now toggle between three states for race icons:', [
        '1./default: show LogosMod team icon with race icon as fallback',
        '2. hide race icon',
        '3. show race icon',
      ]),
      'Control+Shift+L hotkey has been removed as its functionality is now included in Control+Shift+R',
      'fixed minimap preview of replays being cut off',
      "increased width of observer toolbar's vision dropdown",
      '"1" is now hidden in the units/structures lost panels and the leaderpanel except for the Upgrades tab',
      'Seeking in replays now hides the map score panel',
      'alert tooltips now follow the dark color scheme',
      'many labels now scale instead of being truncated',
      "added basic support for Ahli's 'Observer UI Settings Editor'",
      'fixed EPM (Control+V) background image not extending behind the title label',
      "added hotkey to toggle the chat's visibility (Control + Alt + C)",
      'added hotkey to toggle the entire UI (Control + Alt + U)',
      'toggling the observer toolbar will now move the top center panels downwards',
      'fixed 1vs1 comparison frames (e.g. units/workers killed on default Control+R) not hiding graphs panels (created by GameHeart extension mod)',
      "fixed chat bar's chat help tooltip using the old green background color on first UI load",
      "added blur effect to the bottom panel's background images",
      "fixed active forces comparison bars' height",
      "hid chat bar's help label that usually appears below the bar",
      'the leaderpanel moves downwards if the top name panel is shown',
      're-added the unit rank labels',
    ],
  } as Changelog,
];
