import type {ReactNode} from 'react';
import clsx from 'clsx';
import Link from '@docusaurus/Link';
import useDocusaurusContext from '@docusaurus/useDocusaurusContext';
import Layout from '@theme/Layout';
import HomepageFeatures from '@site/src/components/HomepageFeatures';
import Heading from '@theme/Heading';

import styles from './index.module.css';

function HomepageHeader() {
  const {siteConfig} = useDocusaurusContext();
  return (
    <header className={clsx('hero hero--primary', styles.heroBanner)}>
      <div className="container">
        <Heading as="h1" className="hero__title">
          {siteConfig.title}
        </Heading>
        <p className="hero__subtitle">{siteConfig.tagline}</p>
        <div className={styles.buttons}>
          <Link
            className="button button--secondary button--lg"
            to="/docs/intro">
            Docusaurus Tutorial - 5min ⏱️
          </Link>
        </div>
      </div>
    </header>
  );
}

export default function Home(): ReactNode {
  const {siteConfig} = useDocusaurusContext();
  return (
    <Layout
      title="Runix"
      description="A reactive cognitive framework for real-time systems"
    >
      <main className={styles.main}>
        <section className={styles.hero}>
        <img src="/img/robot.png" alt="Robotics optimized" className={styles.cardImage} />
          <h1>Runix</h1>
          <p className={styles.tagline}>
            A cognitive engine for real-time reasoning and behavior
          </p>
          <Link className="button button--primary" to="/docs/getting-started">
            Learn More
          </Link>
        </section>

        <section className={styles.features}>
          <div className={styles.card}>
            <h3>Build complex behaviors, fast</h3>
            <p>
              Runix turns time-aware logic into structured, declarative building blocks. No polling. No timers. No reactive mess. Just clear, composable behavior.
            </p>
          </div>
          <div className={styles.card}>
            <h3>Designed for Robotics</h3>
            <p>
              Conditions track memory, persistence, silence, and frequency. It’s not just if something is true—it’s when, how long, and what came before.
            </p>
          </div>
          <div className={styles.card}>
            <h3>Traceable Behaviors</h3>
            <p>
              Every signal, every reaction, every outcome—structured, timestamped, and logged. You always know why the system did what it did.
            </p>
          </div>
        </section>
      </main>
    </Layout>
  );
}
